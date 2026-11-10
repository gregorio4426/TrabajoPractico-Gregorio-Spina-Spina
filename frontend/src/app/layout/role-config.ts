import { IconName } from '../shared/icon';
import { Role } from '../core/auth.models';
export interface RoleConfig { label: string; workspace: string; title: string; description: string; icon: IconName; pillars: readonly string[]; }
export const ROLE_CONFIG: Record<Role, RoleConfig> = {
  ADMIN: { label: 'Administración', workspace: 'CONTROL DEL GIMNASIO', title: 'Un equipo, una dirección.', description: 'Este es tu espacio de administración. Tu sesión está lista para gestionar el gimnasio.', icon: 'grid', pillars: ['Personas', 'Organización', 'Seguimiento'] },
  PROFESOR: { label: 'Profesor', workspace: 'ESPACIO DEL PROFESOR', title: 'Acompañá cada avance.', description: 'Este es tu espacio de trabajo. Tu sesión está lista para acompañar a tus alumnos.', icon: 'arrow', pillars: ['Planificación', 'Acompañamiento', 'Evolución'] },
  ALUMNO: { label: 'Alumno', workspace: 'MI ENTRENAMIENTO', title: 'El progreso empieza con vos.', description: 'Este es tu espacio personal. Tu sesión está lista para continuar tu camino en el gimnasio.', icon: 'bolt', pillars: ['Constancia', 'Entrenamiento', 'Progreso'] },
};
