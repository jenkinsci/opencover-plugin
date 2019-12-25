package io.jenkins.plugins.opencover;

import hudson.FilePath;
import io.jenkins.plugins.coverage.CoverageAction;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class OpenCoverReportAdapterTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void StandardReportTest() throws Exception {
        // TODO: rename cobertura report
        String coberturaReport = "opencover.xml";
        StringBuilder sb = new StringBuilder();
        sb.append("node {")
                .append("publishCoverage(");

        sb.append("adapters:[");

        sb.append(String.format("opencoverAdapter(path: '%s')], sourceFileResolver: sourceFiles('NEVER_STORE')", coberturaReport));
        sb.append(")").append("}");

        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child(coberturaReport)
                .copyFrom(getClass().getResourceAsStream(coberturaReport));

        project.setDefinition(new CpsFlowDefinition(sb.toString(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();
        Assert.assertNotNull(r);
        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        CoverageAction coverageAction = r.getAction(CoverageAction.class);

        Ratio lineCoverage = coverageAction.getResult().getCoverage(CoverageElement.LINE);
        Assert.assertEquals(lineCoverage.toString(),"122/138");

        Ratio branchCoverage = coverageAction.getResult().getCoverage(CoverageElement.CONDITIONAL);
        Assert.assertEquals(branchCoverage.toString(),"35/48");
    }

    @Test
    public void SourceFileTest() throws Exception {
        String opencoverReport = "reporttotestsourcefiles.xml";

        StringBuilder sb = new StringBuilder();
        sb.append("node {")
                .append("publishCoverage(");

        sb.append("adapters:[");

        sb.append(String.format("opencoverAdapter(path: '%s')], sourceFileResolver: sourceFiles('STORE_LAST_BUILD')", opencoverReport));
        sb.append(")").append("}");

        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        String sourceFile = "testsourcefiles.cs";
        Objects.requireNonNull(workspace)
                .child(sourceFile)
                .copyFrom(getClass().getResourceAsStream(sourceFile));

        String opencoverReportContent =
                new String(Files.readAllBytes(Paths.get(getClass().getResource(opencoverReport).toURI())));
        String sourceFileWorkspacePath = workspace.getRemote().concat("/" + sourceFile);
        opencoverReportContent =
                opencoverReportContent.replace("[PLACEHOLDER]", sourceFileWorkspacePath);
        workspace.child(opencoverReport).copyFrom(new ByteArrayInputStream(opencoverReportContent.getBytes(StandardCharsets.UTF_8)));

        project.setDefinition(new CpsFlowDefinition(sb.toString(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();
        Assert.assertNotNull(r);
        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        CoverageAction coverageAction = r.getAction(CoverageAction.class);

        CoverageResult coverageResult = coverageAction.getResult();
        Ratio lineCoverage = coverageResult.getCoverage(CoverageElement.LINE);
        Assert.assertEquals(lineCoverage.toString(),"9/15");

        CoverageResult moduleCoverageResult = coverageResult.getChild("OpenCover coverage: reporttotestsourcefiles.xml").getChild("ClassLibrary");
        CoverageResult methodCoverageResult = moduleCoverageResult.getChild("ClassLibrary.LibraryClass").getChild("System.Int32 ClassLibrary.LibraryClass::Sum(System.Int32,System.Int32)");
        Assert.assertEquals(methodCoverageResult.isSourceFileAvailable(), true);
    }

    @Test
    public void ReportWIthSkippedModules() throws Exception {
        String opencoverReport = "opencoverwithskippedmodules.xml";
        StringBuilder sb = new StringBuilder();
        sb.append("node {")
                .append("publishCoverage(");

        sb.append("adapters:[");

        sb.append(String.format("opencoverAdapter(path: '%s')], sourceFileResolver: sourceFiles('NEVER_STORE')", opencoverReport));
        sb.append(")").append("}");

        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child(opencoverReport)
                .copyFrom(getClass().getResourceAsStream(opencoverReport));

        project.setDefinition(new CpsFlowDefinition(sb.toString(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();
        Assert.assertNotNull(r);
        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        CoverageAction coverageAction = r.getAction(CoverageAction.class);

        Ratio lineCoverage = coverageAction.getResult().getCoverage(CoverageElement.LINE);
        Assert.assertEquals(lineCoverage.toString(),"1577/1657");

        Ratio branchCoverage = coverageAction.getResult().getCoverage(CoverageElement.CONDITIONAL);
        /*
            Would be null because older reports didn't contain the "sl" attribute
            which we use to process lines
        */
        Assert.assertNull(branchCoverage);
    }
}