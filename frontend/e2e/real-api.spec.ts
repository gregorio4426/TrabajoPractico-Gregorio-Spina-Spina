import { test, expect } from '@playwright/test';
import { randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';

test.describe('Real backend integration (no intercepted responses)', () => {
  test.skip(!process.env['GYM_E2E_REAL_API'], 'Set GYM_E2E_REAL_API=1 with an isolated running backend.');
  test('ALUMNO: register, login, real JWT, authenticated profile, reload and logout',async ({page,request}) => {
    const email=`e2e-${randomBytes(6).toString('hex')}@example.test`;
    const password=randomBytes(18).toString('base64url')+'A1!';
    const registered=await request.post('/api/auth/register/alumno',{data:{email,password,nombre:'Prueba',apellido:'Angular',edad:25,fechaNacimiento:'2001-01-01',nivel:'PRINCIPIANTE',objetivo:'MEJORAR_RESISTENCIA'}});
    expect(registered.status()).toBe(201);
    await page.goto('/login');await page.getByLabel('Email',{exact:true}).fill(email);
    await page.getByLabel('Contraseña',{exact:true}).fill(password);
    await page.getByRole('button',{name:'Iniciar sesión'}).click();
    await expect(page).toHaveURL(/\/alumno$/);await expect(page.getByText(email)).toBeVisible();
    const profile=await page.evaluate(async () => {
      const session=JSON.parse(localStorage.getItem('gym.session.v1') ?? '{}') as {token:string};
      const response=await fetch('/api/alumnos/me',{headers:{Authorization:'Bearer '+session.token}});
      return {status:response.status,email:(await response.json() as {email:string}).email};
    });
    expect(profile).toEqual({status:200,email});
    await page.reload();await expect(page.getByText(email)).toBeVisible();
    await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();
    await page.getByRole('dialog').getByRole('button',{name:'Cerrar sesión',exact:true}).click();
    await expect(page).toHaveURL(/\/login$/);
  });
  for(const role of ['ADMIN','PROFESOR'] as const) {
    test(`${role}: real login and backend-authorized profile`,async ({page,request}) => {
      test.skip(!process.env['GYM_E2E_CREDENTIALS_FILE'],'Provide a private credentials file for isolated test accounts.');
      const credentials=JSON.parse(readFileSync(process.env['GYM_E2E_CREDENTIALS_FILE']!,'utf8')) as Record<string,{email:string;password:string}>;
      const account=credentials[role]!;
      await page.goto('/login');await page.getByLabel('Email',{exact:true}).fill(account.email);
      await page.getByLabel('Contraseña',{exact:true}).fill(account.password);
      await page.getByRole('button',{name:'Iniciar sesión'}).click();
      await expect(page).toHaveURL(new RegExp('/'+role.toLowerCase()+'$'));
      await expect(page.getByText(account.email)).toBeVisible();
      const token=await page.evaluate(()=> (JSON.parse(localStorage.getItem('gym.session.v1')??'{}') as {token:string}).token);
      const response=await request.get(role==='ADMIN'?'/api/admin/resumen':'/api/profesores/me',{headers:{Authorization:'Bearer '+token}});
      expect(response.status()).toBe(200);
    });
  }
});
