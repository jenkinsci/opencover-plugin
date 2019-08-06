package io.jenkins.plugins.opencover;

import hudson.FilePath;
import io.jenkins.plugins.coverage.CoverageAction;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.Ratio;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Objects;

public class OpenCoverReportAdapterTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void StandardReportTest() throws Exception {
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
}