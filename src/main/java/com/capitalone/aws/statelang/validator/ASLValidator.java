package com.capitalone.aws.statelang.validator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Goal which touches a timestamp file.
 */
@Mojo( name = "statelangvalidator", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class ASLValidator 
    extends AbstractMojo
{
    /**
     * Location of the file.
     */
    @Parameter(  property = "outputDirectory", required = true )
    private File outputDirectory;
    private List<SchemaValidationError> validationErrors=new ArrayList<>();

    @Parameter(property = "inputFileName",required = true)
    private String inputFileName;

    public void execute()
        throws MojoExecutionException
    {
        System.out.println("Inside file");
        System.out.println(inputFileName);
        System.out.println(outputDirectory);
        File f = new File(inputFileName);

        try(InputStream stream = this.getClass().getResourceAsStream("/schemas/state-machine.json")) {
            System.out.println(FileUtils.fileRead(f.getAbsolutePath()));


            JSONObject schemaJson = new JSONObject(new JSONTokener(stream));
            System.out.println("the schema is " + schemaJson.toString());

            System.out.println("the definition is" + f.getAbsolutePath());
            String json = FileUtils.fileRead(f.getAbsolutePath());
            System.out.println("The json is"  + json);
            JSONObject inputRequestJson=new JSONObject(json);
            System.out.println("the inputreqjson "+ inputRequestJson.toString());

            validateAgainstSchema(inputRequestJson,schemaJson);
        }
        catch(Exception exp){
            exp.printStackTrace();
            throw new MojoExecutionException("fdsfdfdf");
        }

        if ( !f.exists() )
        {
            throw new MojoExecutionException( "Step Function Definition does not exist" );
        }
        if(!this.validationErrors.isEmpty())
        {
            throw new MojoExecutionException( "Step Function Definition is not valid" );

        }

    }

    private void validateAgainstSchema(JSONObject inputRequestJson,JSONObject schemaPath) throws  Exception{

        SchemaLoader loader = SchemaLoader.builder()
                .schemaClient(SchemaClient.classPathAwareClient())
                .schemaJson(schemaPath)
                .resolutionScope("classpath://schemas/") // setting the default resolution scope
                .build();

        Schema schema = loader.load().build();

        try {
            schema.validate(inputRequestJson);
        }
        catch (ValidationException e) {

            formatErrorToJSON(e.toJSON());
            handleNestedValidationException(e.getCausingExceptions());
            e.getCausingExceptions().stream()
                    .map(ValidationException::getMessage)
                    .forEach(System.out::println);
        }
        catch (Exception exp){
            exp.printStackTrace();
        }
    }

    private void handleNestedValidationException(List<ValidationException> e){
        e.forEach((exception) -> {
            formatErrorToJSON(exception.toJSON());
            exception.getCausingExceptions().stream().forEach((exp) -> {
                formatErrorToJSON(exp.toJSON());
                handleNestedValidationException(exp.getCausingExceptions());
            });
        });
    }


    private void formatErrorToJSON(JSONObject messageError){
        if(messageError.has("keyword")) {
            String keyword = messageError.getString("keyword");
            String element = messageError.getString("pointerToViolation");
            String errorDescription = messageError.getString("message");

            SchemaValidationError error = new SchemaValidationError();
            error.setElement(element);
            error.setKeyword(keyword);
            error.setErrorDescription(errorDescription);
            this.validationErrors.add(error);
            System.out.println(String.format("keyword: {%s} element {%s} errorDesc {%s} ", keyword, element, errorDescription));
        }
    }
}
