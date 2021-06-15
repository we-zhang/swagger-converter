package io.swagger.handler;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.oas.inflector.models.RequestContext;
import io.swagger.oas.inflector.models.ResponseContext;
import io.swagger.parser.OpenAPIParser;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaInflectorServerCodegen", date = "2017-04-08T15:48:56.501Z")
public class ConverterController {
    public ResponseContext convertByContent(RequestContext request, JsonNode inputSpec) {
        if(inputSpec == null) {
            return new ResponseContext()
                    .status(Response.Status.BAD_REQUEST)
                    .entity( "No specification supplied in either the url or request body.  Try again?" );
            }
        String inputAsString = Json.pretty(inputSpec);
        SwaggerParseResult output = new OpenAPIParser().readContents(inputAsString, null, null);
        if(output == null) {
            return new ResponseContext().status(Response.Status.INTERNAL_SERVER_ERROR).entity( "Failed to process URL" );
        }
        if(output.getOpenAPI() == null) {
            return new ResponseContext().status(Response.Status.BAD_REQUEST).entity(output.getMessages());
        }


        MediaType outputType = getMediaType(request);

        OpenAPI oa = output.getOpenAPI();

        // Fix issue https://github.com/swagger-api/swagger-converter/issues/60
        Paths paths = oa.getPaths();
        for (PathItem item: paths.values()) {
            if (item.getPost() != null && item.getPost().getRequestBody() != null && item.getPost().getRequestBody().getContent() != null) {
                Content c = item.getPost().getRequestBody().getContent();
                if (c.get("multipart/form-data") != null && c.get("multipart/form-data").getSchema() != null) {
                    c.get("multipart/form-data").getSchema().setType("object");
                }
            }
            if (item.getPut() != null && item.getPut().getRequestBody() != null && item.getPut().getRequestBody().getContent() != null) {
                Content c = item.getPut().getRequestBody().getContent();
                if (c.get("multipart/form-data") != null && c.get("multipart/form-data").getSchema() != null) {
                    c.get("multipart/form-data").getSchema().setType("object");

                    if (item.getPut().getOperationId().equals("updateVariables")) {

                        // Inner basic type shcmea
                        Schema basicSchema = new Schema();
                        basicSchema.setType("string");
                        basicSchema.setFormat("binary");

                        Map<String, Schema> props = new HashMap<>();
                        props.put("variables", basicSchema);

                        ArrayList<String> required = new ArrayList<>();
                        required.add("variables");

                        Schema formSchema = new Schema();
                        formSchema.setType("object");
                        formSchema.setProperties(props);
                        formSchema.setRequired(required);
                        c.get("multipart/form-data").setSchema(formSchema);

//                        c.get("multipart/form-data").getSchema().setProperties(props);
//                        c.get("multipart/form-data").getSchema().required(required);

                    }
                }
            }
        }

        // Update authorization security from 2.0 apiKey to 3.0 http-bearer
        Map<String, SecurityScheme> ss = oa.getComponents().getSecuritySchemes();
        if (ss != null) {
            SecurityScheme bearer = ss.get("OAuth2Bearer");
            if (bearer != null) {
                bearer.setType(SecurityScheme.Type.HTTP);
                bearer.setScheme("bearer");
                bearer.setName(null);
                bearer.setIn(null);
            }
        }

        return new ResponseContext()
                .contentType(outputType)
                .entity(oa);
    }

    public ResponseContext convertByUrl(RequestContext request , String url) {
        if(url == null) {
            return new ResponseContext()
                    .status(Response.Status.BAD_REQUEST)
                    .entity( "No specification supplied in either the url or request body.  Try again?" );
        }
        SwaggerParseResult output = new OpenAPIParser().readLocation(url, null, null);
        if(output == null) {
            return new ResponseContext().status(Response.Status.INTERNAL_SERVER_ERROR).entity( "Failed to process URL" );
        }
        if(output.getOpenAPI() == null) {
            return new ResponseContext().status(Response.Status.BAD_REQUEST).entity(output.getMessages());
        }

        MediaType outputType = getMediaType(request);

        return new ResponseContext()
                .contentType(outputType)
                .entity(output.getOpenAPI());
    }

    private MediaType getMediaType(RequestContext request) {
        MediaType outputType = MediaType.APPLICATION_JSON_TYPE;

        boolean isJsonOK = false;
        boolean isYamlOK = false;

        MediaType yamlMediaType = new MediaType("application", "yaml");

        for (MediaType mediaType : request.getAcceptableMediaTypes()) {
            if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
                isJsonOK = true;
            } else if (mediaType.equals(yamlMediaType)) {
                isYamlOK = true;
            }
        }

        if (isYamlOK && !isJsonOK) {
            outputType = yamlMediaType;
        }

        return outputType;
    }
}

