package vanilla.bdd;

import java.util.List;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.reporters.Format;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;

/**
 * JBehave runner for the post-deploy verification suite.
 *
 * <p>Runs only the non-invasive {@code stories/verification/} stories against a live pod via its
 * candidate Service URL. Registered step class is {@link VerificationSteps} only — {@link
 * ProductCrudSteps} is intentionally excluded to prevent invasive CRUD operations against a pod
 * that is still being qualified for production traffic.
 */
public class VerificationStories extends JUnitStories {

  public VerificationStories() {
    configuredEmbedder()
        .embedderControls()
        .doGenerateViewAfterStories(true)
        .doIgnoreFailureInStories(false)
        .doIgnoreFailureInView(true);
  }

  @Override
  public Configuration configuration() {
    return new MostUsefulConfiguration()
        .useStoryLoader(new LoadFromClasspath(this.getClass()))
        .useStoryReporterBuilder(
            new StoryReporterBuilder()
                .withDefaultFormats()
                .withFormats(Format.CONSOLE, Format.TXT));
  }

  @Override
  public InjectableStepsFactory stepsFactory() {
    return new InstanceStepsFactory(configuration(), new VerificationSteps());
  }

  @Override
  public List<String> storyPaths() {
    return List.of("stories/verification/app_verification.story");
  }
}
