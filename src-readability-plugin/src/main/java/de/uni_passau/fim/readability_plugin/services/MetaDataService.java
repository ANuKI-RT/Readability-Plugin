package de.uni_passau.fim.readability_plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.VirtualFile;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityResult;
import de.uni_passau.fim.readability_plugin.painting.RatedJavaScope;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * For the user study carried out with the readability plugin statistical metadata
 * must be collected while the plugin is in use, in order to analyze the data later.
 *
 * This service class can be used to save this statistical data in a file.
 * The data is stored in a xml format in the .idea/readability_meta.xml
 *
 * ------------------------
 * !!! KNOWN ISSUES !!!
 * There is known bug when the gradle project is not built yet.
 * In this case the readability_meta.xml file cannot be created and / or
 * no data can be written in the file. Future version must fix this issue.
 *
 * For a workaround the statistical data is also written in the IDEA logs in order to
 * extract it from there.
 *
 * TODO: Fix the bug that prevents writing the .idea/readability_meta.xml file
 *
 * ------------------------
 */
@Service(Service.Level.PROJECT)
public final class MetaDataService {

    private Project project;
    private VirtualFile metaFile;

    private boolean fileInitiated;

    MetaDataService(Project project) {
        this.project = project;
        fileInitiated = false;

    }

    /**
     * Initializes the service by creating the readability_meta.xml
     * file if it does nox exist yet.
     * After the file was created (or the file was found if it already exists)
     * a reference to the file is stored in the "metaFile" class attribute.
     */
    public void init() {

        try {
            VirtualFile workSpace = project.getWorkspaceFile();

            if(workSpace == null) {
                return;
            }

            VirtualFile ideaFolder = workSpace.getParent();

            metaFile = ideaFolder.findChild("readability_meta.xml");
            if (metaFile == null || !new File(metaFile.getPath()).exists()) {
                createMetaFile(ideaFolder);
            }
            else {
                fileInitiated = true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * Creates an empty readability_meta.xml file in the .idea folder.
     * @param ideaFolder the path pointing to the .idea folder of the opened project
     */
    private void createMetaFile(VirtualFile ideaFolder){
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    metaFile = ideaFolder.createChildData(this, "readability_meta.xml");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            SwingUtilities.invokeLater(() -> {
                try {
                    Document document = new Document(new Element("metadata"));
                    writeMetaFile(document);

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            });

    }

    /**
     * Update the readability_meta.xml by writing the data from the given metaData element in it.
     * @param metaData the statistical metadata to add to the file
     */
    private void updateMetaFile(Element metaData) {
        try {
            logElement(metaData);
            if(!fileInitiated) {
                return;
            }
            // Update Swing components on the UI thread
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            SwingUtilities.invokeLater(() -> {
                try {
                    addElementValue(metaData, "timestamp", timestamp.toString());

                    Element root = JDOMUtil.load(new File(metaFile.getPath()));
                    root.addContent(metaData);

                    writeMetaFile(new Document(root));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs the content of a statistical metadata object in the console.
     * The content is logged in xml format and wrapped in a statistics tag.
     * The statistics tag does also include a parameter to store the time when the data was logged.
     * @param metaData the meta data to log to the console
     */
    private void logElement(Element metaData ) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = now.format(formatter);
        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(Format.getPrettyFormat()); // Optional: To get pretty formatted XML
        String xmlString = xmlOutputter.outputString(metaData);
        System.out.println("<statistics time=\""+timestamp+"\">"+xmlString+"</statistics>");

    }

    /**
     * Takes an updated version of the Document describing the content of the readability_meta.xml file
     * and overwrites the file with this content
     * @param metaDocument the content that should be written in the readability_meta.xml file
     */
    private void writeMetaFile(Document metaDocument) {

        ApplicationManager.getApplication().runWriteAction(() -> {

            try {
                Format format = Format.getPrettyFormat();
                format.setExpandEmptyElements(true);
                XMLOutputter xmlOutputter = new XMLOutputter(format);
                FileOutputStream fileOutputStream = new FileOutputStream(metaFile.getPath());
                xmlOutputter.output(metaDocument, fileOutputStream);
                fileOutputStream.close();
                fileInitiated = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

        });

    }

    /**
     * Write a new "gutter icon hover" event to the statistical metadata file.
     * @param ratedJavaScope the method associated to the gutter icon the user hovered over
     */
    public void registerGutterIconHover(RatedJavaScope ratedJavaScope) {
        Element gutterHoverMeta = new Element("data");
        gutterHoverMeta.setAttribute("meta-type", "gutter-hover");

        addElementValue(gutterHoverMeta,"java-file-path", ratedJavaScope.getJavaFilePath());
        addElementValue(gutterHoverMeta,"method-name", ratedJavaScope.getMethodName());
        addElementValue(gutterHoverMeta,"rating",Double.toString(ratedJavaScope.getRating()));
        addElementValue(gutterHoverMeta,"start-line",Integer.toString(ratedJavaScope.getStartIndex()));
        addElementValue(gutterHoverMeta,"end-line",Integer.toString(ratedJavaScope.getEndIndex()));

        updateMetaFile(gutterHoverMeta);
    }

    /**
     * Add a new tag to a xml element
     * @param element the element to add the tag to
     * @param key the key defining the tag name
     * @param value the value to be set for the tag
     */
    private void addElementValue(Element element,String key, String value) {
        Element childElement = new Element(key);
        childElement.addContent(value);
        element.addContent(childElement);
    }

    /**
     * Write a new "triggered readability details" event to the statistical metadata file.
     * @param ratedJavaScope the java method that used did request readability details for
     * @param detailAction the feature that was triggered using the details menu (readability tree of readability hint)
     */
    public void registerDisplayDetails(RatedJavaScope ratedJavaScope, String detailAction)  {

        Element displayDetailsMeta = new Element("data");
        displayDetailsMeta.setAttribute("meta-type", "display-details");

        addElementValue(displayDetailsMeta,"java-file-path", ratedJavaScope.getJavaFilePath());
        addElementValue(displayDetailsMeta,"detail-action", detailAction);
        addElementValue(displayDetailsMeta,"method-name", ratedJavaScope.getMethodName());
        addElementValue(displayDetailsMeta,"rating",Double.toString(ratedJavaScope.getRating()));
        addElementValue(displayDetailsMeta,"start-line",Integer.toString(ratedJavaScope.getStartIndex()));
        addElementValue(displayDetailsMeta,"end-line",Integer.toString(ratedJavaScope.getEndIndex()));

        updateMetaFile(displayDetailsMeta);


    }

    /**
     * Write a new "readability gutter icons were re-rendered" event to the statistical metadata file.
     * @param ratedJavaScope the java methods which readability was re evaluated in order to update the gutter icons
     */
    public void registerReadabilityReRender(RatedJavaScope ratedJavaScope) {
        Element reRenderMeta = new Element("data");
        reRenderMeta.setAttribute("meta-type", "readability-re-render");

        addElementValue(reRenderMeta,"java-file-path", ratedJavaScope.getJavaFilePath());
        addElementValue(reRenderMeta,"method-name", ratedJavaScope.getMethodName());
        addElementValue(reRenderMeta,"rating",Double.toString(ratedJavaScope.getRating()));
        addElementValue(reRenderMeta,"start-line",Integer.toString(ratedJavaScope.getStartIndex()));
        addElementValue(reRenderMeta,"end-line",Integer.toString(ratedJavaScope.getEndIndex()));

        updateMetaFile(reRenderMeta);
    }

    /**
     * Write a new "user started rating of marked code" event to the statistical metadata file
     * As the user has the option of cancelling the feature, 3 separate statistical data points must be added for the
     * "started" , "cancelled" and "finished" events
     *
     * @param codeSnippet the code snippet that was rated when using the feature
     */
    public void registerStartMarkedCodeRating(String codeSnippet) {
        Element startCodeMarkingMeta = new Element("data");

        String encodedString = Base64.getEncoder().encodeToString(codeSnippet.getBytes());
        startCodeMarkingMeta.setAttribute("start-code-marking", encodedString);

        updateMetaFile(startCodeMarkingMeta);
    }

    /**
     * Write a new "user canceled rating of marked code" event to the statistical metadata file
     * As the user has the option of cancelling the feature, 3 separate statistical data points must be added for the
     * "started" , "cancelled" and "finished" events
     *
     * @param codeSnippet the code snippet that should have been rated using the feature
     */
    public void registerCancelMarkedCodeRating(String codeSnippet) {
        Element cancelCodeMarkingMeta = new Element("data");

        String encodedString = Base64.getEncoder().encodeToString(codeSnippet.getBytes());
        cancelCodeMarkingMeta.setAttribute("cancel-code-marking", encodedString);

        updateMetaFile(cancelCodeMarkingMeta);
    }

    /**
     * Write a new "user finished rating of marked code" event to the statistical metadata file
     * As the user has the option of cancelling the feature, 3 separate statistical data points must be added for the
     * "started" , "cancelled" and "finished" events
     *
     * @param codeSnippet the code snippet that was successfully rated using the feature
     * @param result the readability assessment result for the marked snippet
     */
    public void registerFinishedMarkedCodeRating(String codeSnippet,ReadabilityResult result) {
        Element finishCodeMarkingMeta = new Element("data");

        String encodedString = Base64.getEncoder().encodeToString(codeSnippet.getBytes());
        finishCodeMarkingMeta.setAttribute("finish-code-marking", encodedString);

        updateMetaFile(finishCodeMarkingMeta);
    }

}
