package utn.simulacro_nombreAlumno.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

@Configuration
@OpenAPIDefinition(info=@Info(title="Gimnasio API",version="1.0.0",description="Backend para ADMIN, PROFESOR y ALUMNO con login único. No existe herencia implícita de roles. Historial versionado, cuentas revocables y estadísticas de datos reales. Las consultas estadísticas usan fechas efectivas inclusivas y pesos en la unidad registrada; no calculan asistencia, duración ni medidas corporales."),
        security=@SecurityRequirement(name="bearerAuth"))
@SecurityScheme(name="bearerAuth",type=SecuritySchemeType.HTTP,scheme="bearer",bearerFormat="JWT")
public class OpenApi {
    @Bean
    OperationCustomizer permisosDocumentados() {
        return (operation,handler)-> {
            PreAuthorize rule=handler.getMethodAnnotation(PreAuthorize.class);
            if(rule==null)rule=handler.getBeanType().getAnnotation(PreAuthorize.class);
            if(rule!=null) {
                var matcher=java.util.regex.Pattern.compile("'(ADMIN|PROFESOR|ALUMNO)'").matcher(rule.value());
                java.util.Set<String> roles=new java.util.LinkedHashSet<>();while(matcher.find())roles.add(matcher.group(1));
                String note="Roles admitidos: "+String.join(", ",roles)+". También se verifican propiedad y vinculaciones actuales en los servicios cuando corresponde.";
                operation.setDescription(operation.getDescription()==null?note:operation.getDescription()+"\n\n"+note);
            }
            return operation;
        };
    }
    @Bean
    OpenApiCustomizer erroresYSecretosDocumentados() {
        return api->{
            if(api.getPaths()!=null) api.getPaths().values().forEach(path->path.readOperationsMap().forEach((method,op)->{
                if(method!=io.swagger.v3.oas.models.PathItem.HttpMethod.GET && !op.getResponses().containsKey("409"))
                    op.getResponses().addApiResponse("409",new io.swagger.v3.oas.models.responses.ApiResponse().description("Conflicto de estado, revisión o integridad")
                        .content(new io.swagger.v3.oas.models.media.Content().addMediaType("application/json",new io.swagger.v3.oas.models.media.MediaType().schema(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/ErrorResponse")))));
                for(var entry:java.util.Map.of("400","Solicitud inválida","401","Autenticación requerida o credenciales inválidas","403","Acceso no permitido","404","Recurso no encontrado","500","Error interno sin detalles del servidor").entrySet()) {
                    if(!op.getResponses().containsKey(entry.getKey()))op.getResponses().addApiResponse(entry.getKey(),new io.swagger.v3.oas.models.responses.ApiResponse().description(entry.getValue())
                            .content(new io.swagger.v3.oas.models.media.Content().addMediaType("application/json",new io.swagger.v3.oas.models.media.MediaType().schema(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/ErrorResponse")))));
                }
            }));
            if(api.getComponents()!=null && api.getComponents().getSchemas()!=null) api.getComponents().getSchemas().values().forEach(schema->{
                if(schema.getProperties()!=null) schema.getProperties().forEach((name,property)->{
                    String field=name.toString();var p=(io.swagger.v3.oas.models.media.Schema<?>)property;
                    if(java.util.Set.of("password","passwordActual","nuevaPassword").contains(field)){p.setWriteOnly(true);p.setFormat("password");}
                });
            });
        };
    }
}
