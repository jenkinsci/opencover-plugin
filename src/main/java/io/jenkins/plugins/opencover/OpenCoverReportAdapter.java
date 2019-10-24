package io.jenkins.plugins.opencover;

import com.google.common.collect.Lists;
import hudson.Extension;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.parser.CoverageParser;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OpenCoverReportAdapter extends CoverageReportAdapter {

    private static final String COVERAGE_ELEMENT_TYPE = "OpenCover";

    @DataBoundConstructor
    public OpenCoverReportAdapter(String path) {
        super(path);
    }

    @Override
    public Document convert(File source) throws CoverageException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        String FEATURE = null;
        Document document = null;

        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
            factory.setFeature(FEATURE, true);

            FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            factory.setFeature(FEATURE, false);

            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();


            document = builder.newDocument();
            Element rootElement = document.createElement("report");
            rootElement.setAttribute("name", "OpenCover coverage");
            document.appendChild(rootElement);

            Document openCoverReport = builder.parse(source);

            Element openCoverReportDocumentElement = openCoverReport.getDocumentElement();

            openCoverReportDocumentElement.normalize();

            NodeList modulesList = openCoverReportDocumentElement.getElementsByTagName("Modules").item(0).getChildNodes();

            for (int moduleIndex = 0; moduleIndex < modulesList.getLength(); moduleIndex++) {
                Node module = modulesList.item(moduleIndex);
                if (module.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element moduleElement = processModule(document, module);
                rootElement.appendChild(moduleElement);
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            throw new CoverageException(e);
        }

        return document;
    }

    private Element processModule(Document document, Node module) {
        Element moduleElementOpenCover = (Element) module;
        Element moduleElement = document.createElement("module");

        String moduleName = moduleElementOpenCover.getElementsByTagName("ModuleName").item(0).getTextContent();
        moduleElement.setAttribute("name", moduleName);

        Node classesNode = moduleElementOpenCover.getElementsByTagName("Classes").item(0);
        NodeList listOfClasses = classesNode.getChildNodes();

        for (int classIndex = 0; classIndex < listOfClasses.getLength(); classIndex++) {
            Node classNode = listOfClasses.item(classIndex);
            if (classNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element classElement = processClass(document, classNode);
            moduleElement.appendChild(classElement);
        }
        return  moduleElement;
    }

    private Element processClass(Document document, Node classNode) {
        Element openCoverClassElement = (Element) classNode;
        String classFullName = openCoverClassElement.getElementsByTagName("FullName").item(0).getTextContent();

        Element classElement = document.createElement("class");
        classElement.setAttribute("name", classFullName);

        Node methodsNode = openCoverClassElement.getElementsByTagName("Methods").item(0);
        NodeList listOfMethods = methodsNode.getChildNodes();

        for (int methodIndex = 0; methodIndex < listOfMethods.getLength(); methodIndex++) {
            Node methodNode = listOfMethods.item(methodIndex);
            if (methodNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element methodElement = processMethod(document, methodNode);
            classElement.appendChild(methodElement);
        }
        return classElement;
    }

    private Element processMethod(Document document, Node methodNode) {
        Element methodElement = document.createElement("method");
        Element openCoverMethodElement = (Element) methodNode;
        String methodFullName = openCoverMethodElement.getElementsByTagName("Name").item(0).getTextContent();
        methodElement.setAttribute("name", methodFullName);

        ArrayList<Element> lineElements = processLineElements(document, openCoverMethodElement);

        for (Element lineElement: lineElements) {
            methodElement.appendChild(lineElement);
        }

        return methodElement;
    }

    private ArrayList<Element> processLineElements(Document document, Element openCoverMethodElement) {
        Node sequencePoints = openCoverMethodElement.getElementsByTagName("SequencePoints").item(0);
        NodeList listOfSequencePoints = sequencePoints.getChildNodes();
        ArrayList<Element> lineElements = new ArrayList<Element>();
        for (int sequencePointIndex = 0; sequencePointIndex < listOfSequencePoints.getLength(); sequencePointIndex++) {
            Node sequencePoint = listOfSequencePoints.item(sequencePointIndex);
            if (sequencePoint.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element lineElement = document.createElement("line");
            NamedNodeMap sequencePointAttributes = sequencePoint.getAttributes();
            String sourceLineNumber = sequencePointAttributes.getNamedItem("sl").getTextContent();
            String sourceLineHits = sequencePointAttributes.getNamedItem("vc").getTextContent();
            lineElement.setAttribute("hits", sourceLineHits);
            lineElement.setAttribute("number", sourceLineNumber);

            lineElements.add(lineElement);
        }

        Node branchPoints = openCoverMethodElement.getElementsByTagName("BranchPoints").item(0);
        NodeList listOfBranchPoints = branchPoints.getChildNodes();

        HashMap<String, BranchInfo> branchStatistics = new HashMap<>();

        for (int branchPointIndex = 0; branchPointIndex < listOfBranchPoints.getLength(); branchPointIndex++) {
            Node branchPoint = listOfBranchPoints.item(branchPointIndex);
            if (branchPoint.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            NamedNodeMap branchPointAttributes = branchPoint.getAttributes();

            String sourceLineNumber = branchPointAttributes.getNamedItem("sl").getTextContent();

            BranchInfo branchInfo = branchStatistics.get(sourceLineNumber);
            if (branchInfo == null) {
                branchInfo = new BranchInfo();
                branchStatistics.put(sourceLineNumber, branchInfo);
            }
            branchInfo.setAllBranches(branchInfo.getAllBranches() + 1);
            int branchHits = Integer.parseInt(branchPointAttributes.getNamedItem("vc").getTextContent());
            if (branchHits > 0) {
                branchInfo.setVisitedBranches(branchInfo.getVisitedBranches() + 1);
            }
        }

        for (Map.Entry<String, BranchInfo> entry : branchStatistics.entrySet()) {
            String sourceLineNumber = entry.getKey();
            for (Element lineElement : lineElements) {
                if (lineElement.getAttribute("number").equals(sourceLineNumber)) {
                    lineElement.setAttribute("branch", "true");
                    BranchInfo branchInfo = entry.getValue();
                    int visitedBranches = branchInfo.getVisitedBranches();
                    int allBranches = branchInfo.getAllBranches();
                    int percentOfCoveredBranches = (int)Math.round(100.0 / allBranches * visitedBranches);
                    String conditionCoverage = String.format("%d%% (%d/%d)",
                            percentOfCoveredBranches,
                            visitedBranches,
                            allBranches);
                    lineElement.setAttribute("condition-coverage", conditionCoverage);
                }
            }
        }
        return lineElements;
    }

    @Override
    public CoverageResult parseToResult(Document document, String reportName) throws CoverageException {
        return new OpenCoverCoverageParser(reportName).parse(document);
    }

    @Symbol(value = {"opencoverAdapter"})
    @Extension
    public static final class OpenCoverReportAdapterDescriptor extends CoverageReportAdapterDescriptor<CoverageReportAdapter> {

        public OpenCoverReportAdapterDescriptor() {
            super(OpenCoverReportAdapter.class);
        }

        @Override
        public String getCoverageElementType() {
            return COVERAGE_ELEMENT_TYPE;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.OpenCoverReportAdapter_displayName();
        }

        @Override
        public boolean defaultMergeToOneReport() {
            return true;
        }

        @Override
        public List<CoverageElement> getCoverageElements() {
            return Lists.newArrayList(
                    new CoverageElement("Module", 0),
                    new CoverageElement("Class", 1),
                    new CoverageElement("Method", 2));
        }
    }

    public static final class OpenCoverCoverageParser extends CoverageParser {

        public OpenCoverCoverageParser(String reportName) {
            super(reportName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected CoverageResult processElement(Element current, CoverageResult parentResult) {
            CoverageResult result = null;

            switch (current.getTagName()) {
                case "report":
                    result = new CoverageResult(CoverageElement.REPORT, null,
                            getAttribute(current, "name", "") + ": " + getReportName());
                    break;
                case "module":
                    String moduleName = getAttribute(current, "name");
                    CoverageResult moduleResult = parentResult.getChild(moduleName);
                    if (moduleResult != null) {
                        result = moduleResult;
                    } else {
                        result = new CoverageResult(CoverageElement.get("Module"), parentResult,
                                moduleName);
                    }
                    break;
                case "class":
                    String className = getAttribute(current, "name", "");
                    CoverageResult classResult = parentResult.getChild(className);
                    if (classResult != null) {
                        result = classResult;
                    } else {
                        result = new CoverageResult(CoverageElement.get("Class"), parentResult,
                                className);
                    }
                    break;
                case "method":
                    result = new CoverageResult(CoverageElement.get("Method"), parentResult,
                            getAttribute(current, "name", ""));
                    break;
                case "line":
                    processLine(current, parentResult);
                    break;
                default:
                    break;
            }
            return result;
        }
    }

    private static class BranchInfo {
        private int allBranches;
        private int visitedBranches;
        public BranchInfo() {
            allBranches = 0;
            visitedBranches = 0;
        }

        public int getAllBranches() {
            return allBranches;
        }

        public void setAllBranches(int allBranches) {
            this.allBranches = allBranches;
        }

        public int getVisitedBranches() {
            return visitedBranches;
        }

        public void setVisitedBranches(int visitedBranches) {
            this.visitedBranches = visitedBranches;
        }
    }
}