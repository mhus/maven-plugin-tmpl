/**
 * Copyright 2018 Mike Hummel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mhus.mvn.tmpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import de.mhus.commons.tools.MDate;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MString;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.stringtemplate.v4.ST;

@Mojo(
        name = "tmpl",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        inheritByDefault = true,
        threadSafe = true)
public class TmplMojo extends AbstractMojo {

//    {
//        LogFactory.
//        ((IApiInternal) MApi.get()).setLogFactory(new MavenPluginLogFactory(this));
//    }

    private static Logger LOGGER = Logger.getLogger(TmplMojo.class.getName());

    @Parameter public String filePrefix = "";
    @Parameter public String fileSuffix = "-tmpl";
    @Parameter public String fileExtension = ".tmpl";
    @Parameter public String targetDir = "";
    @Parameter public boolean flattenTargetDir = false;

    /**
     * List of files to include. Specified as fileset patterns which are relative to the input
     * directory whose contents is being parsed.
     */
    @Parameter
    private FileSet files;

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    HashMap<String, Object> parameters = new HashMap<>();

    @Parameter(property = "aggregate", defaultValue = "false")
    public boolean aggregate = false;

    @Parameter
	private char startChar = '±';

    @Parameter
	private char endChar = '±';
	
	private Date now = new Date();

    @Parameter
	private String charset = Charset.defaultCharset().name();

    @Parameter
    private boolean verbose = false;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        System.getenv().forEach((k, v) -> putParameter(k, v));
        System.getProperties().forEach((k, v) -> putParameter(k, v));
        project.getProperties().forEach((k, v) -> putParameter(k, v));
        putParameter("project_version", project.getVersion());
        putParameter("project_groupId", project.getGroupId());
        putParameter("project_artifactId", project.getArtifactId());
        putParameter("project_name", project.getName());
        putParameter("basedir", project.getBasedir());
        if (project.getParent() != null) {
            putParameter("parent_version", project.getParent().getVersion());
            putParameter("parent_groupId", project.getParent().getGroupId());
            putParameter("parent_artifactId", project.getParent().getArtifactId());
        }
        if (verbose)
            new TreeMap<>(parameters).forEach((k, v) -> LOGGER.info("Parameter: " + k + "=" + v));

        List<File> list = toFileList(files);

        for (File file : list) {
            if (file.getPath().contains("/.")) continue;
            if (verbose)
                LOGGER.info("scan " + file);
            String name = MFile.getFileNameOnly(file.getName());
            if ((MString.isSet(filePrefix) || MString.isSet(fileSuffix)) && name.startsWith(filePrefix) && name.endsWith(fileSuffix))
                tmplFile(
                        file,
                        new File(getParent(file.getParent()),
	                        name.substring(filePrefix.length(), name.length() - fileSuffix.length())
	                                + "."
	                                + MFile.getFileExtension(file)));
            else
            if (MString.isSet(fileExtension) && file.getName().endsWith(fileExtension))
                tmplFile(file, new File(getParent(file.getParent()), file.getName().substring(0, file.getName().length() - fileExtension.length()) ));
        }
    }

    private File getParent(String parent) {
        if (MString.isSet(targetDir)) {
            String baseDir = files == null ?  project.getBasedir().getAbsolutePath() : files.getDirectory();
            if (flattenTargetDir) {
                return new File(targetDir);
            }
            String deltaDir = parent.substring(baseDir.length());
            return new File(targetDir + "/" + deltaDir);
        }
        return new File(parent);
    }

    private void putParameter(Object k, Object v) {
        String key = String.valueOf(k);
        Map<String, Object> cont = parameters;
        if (key.contains(".")) {
            String path = MString.beforeLastIndex(key, '.');
            cont = getParamContainer(cont, path);
            key = MString.afterLastIndex(key, '.');
        }
        cont.put(key, v);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getParamContainer(Map<String, Object> parent, String key) {
        Map<String, Object> cont = null;
        if (key.contains(".")) {
            String path = MString.beforeLastIndex(key, '.');
            cont = getParamContainer(parent, path);
            key = MString.afterLastIndex(key, '.');
        } else {
            cont = (Map<String, Object>) parent.get(key);
            if (cont == null) {
                cont = new HashMap<>();
                parent.put(key, cont);
            }
        }

        return cont;
    }

    private void tmplFile(File from, File to) {
        try {
            LOGGER.info("TMPL " + from + " " + to);
            to.getParentFile().mkdirs();
            ST template = new ST(MFile.readFile(from, charset), startChar , endChar );
            parameters.forEach((k,v) -> template.add(k, v));
            template.add("from_name", from.getName());
            template.add("to_name", to.getName());
            template.add("now_datetime", MDate.toIsoDate(now));
            template.add("now_date", MDate.toIsoDateTime(now));
            LOGGER.fine("tmpl attributes " + template.getAttributes());
            String content = template.render();
            FileOutputStream os = new FileOutputStream(to);
            MFile.writeFile(os, content, charset);
            os.close();
        } catch (Throwable e) {
        	LOGGER.warning("Failed " +  e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    public List<File> toFileList(FileSet fs) throws MojoExecutionException {
    	if (fs == null) {
    		fs = new FileSet();
    		fs.setDirectory(project.getBasedir().getAbsolutePath());
    		fs.addInclude("**/**");
    	}
        try {
            if (fs.getDirectory() != null) {
                File directory = new File(fs.getDirectory());
                String includes = toString(fs.getIncludes());
                String excludes = buildExcludes(fs);
                return (List<File>) FileUtils.getFiles(directory, includes, excludes);
            } else {
                getLog().warn(String.format("Fileset [%s] directory empty", fs.toString()));
                return new ArrayList<>();
            }
        } catch (IOException e) {
            throw new MojoExecutionException(
                    String.format("Unable to get paths to fileset [%s]", fs.toString()), e);
        }
    }

    private String buildExcludes(FileSet fs) {
        List<String> ex = new ArrayList<String>();
        ex.addAll(fs.getExcludes());
        if (project != null && project.getModules() != null && !aggregate) {
            for (String module : (List<String>) project.getModules()) {
                ex.add(module + "/**");
            }
        }
        return toString(ex);
    }

    private String toString(List<String> strings) {
        StringBuilder sb = new StringBuilder();
        for (String string : strings) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(string);
        }
        return sb.toString();
    }
}
