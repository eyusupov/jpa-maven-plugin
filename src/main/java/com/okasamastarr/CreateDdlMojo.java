package com.okasamastarr;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Properties;

/**
 * Goal which creates a DDL schema.
 *
 */
@Mojo( name = "create-ddl", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class CreateDdlMojo
    extends AbstractMojo
{
    @Parameter( defaultValue = "${project.build.directory}/schema.sql", property = "outputFile", required = true )
    private String outputFile;

    @Parameter( property = "importFile" )
    private String importFile;

    @Parameter( property = "propFile" )
    private String propFile;

    @Parameter( property = "delimeter" )
    private String delimeter;

    @Parameter( property = "persistenceUnit", required = true )
    private String persistenceUnit;

    @Parameter( defaultValue = "true", property = "format", required = true)
    private boolean format;

    @Parameter( defaultValue = "false", property = "script", required = true)
    private boolean script;

    @Parameter( defaultValue = "false", property = "drop", required = true)
    private boolean drop;

    @Parameter( defaultValue = "true", property = "drop", required = true)
    private boolean create;

    @Parameter( property = "dialect", required = false)
    private String dialect;

    @Parameter( defaultValue = "${project}", readonly = true)
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(createProjectClassLoader());
            Configuration cfg = createHibernateConfiguration(persistenceUnit);

            Properties props = new Properties();
            if (dialect != null) {
                props.setProperty("hibernate.dialect", dialect);
            }
            if (importFile != null) {
                props.setProperty( AvailableSettings.HBM2DDL_IMPORT_FILES, importFile);
            }
            if ( propFile != null ) {
                props.load( new FileInputStream( propFile ) );
            }
            cfg.setProperties(props);

            SchemaExport schemaExport = new SchemaExport(cfg)
                .setOutputFile(outputFile)
                .setFormat(format)
                .setDelimiter(delimeter);
            schemaExport.execute(script, true, drop, create);
        } catch (Throwable ex) {
            throw new MojoExecutionException("Failed to create DDL schema", ex);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @SuppressWarnings("deprecation")
    private Configuration createHibernateConfiguration(String persistenceUnit) throws MalformedURLException {
        return new Ejb3Configuration().configure(persistenceUnit, new Properties()).getHibernateConfiguration();
    }

    private ClassLoader createProjectClassLoader() throws MalformedURLException {
        URL outputDirectory = pathToURL(project.getBuild().getOutputDirectory());
        URLClassLoader projectClassLoader =
                new URLClassLoader(new URL[]{outputDirectory}, Thread.currentThread().getContextClassLoader());
        return projectClassLoader;
    }

    private URL pathToURL(String directory) throws MalformedURLException {
        return new File(directory).toURI().toURL();
    }
}
