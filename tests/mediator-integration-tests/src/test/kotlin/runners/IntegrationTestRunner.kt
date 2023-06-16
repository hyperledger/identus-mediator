package runners

import io.cucumber.junit.CucumberOptions
import net.serenitybdd.cucumber.CucumberWithSerenity
import org.junit.runner.RunWith

@CucumberOptions(
    features = [
        "src/test/resources/features/pickup_message",
    ],
    glue = ["features"],
    snippets = CucumberOptions.SnippetType.CAMELCASE,
    plugin = [
        "pretty",
        "json:target/serenity-reports/cucumber_report.json",
    ],
)
@RunWith(CucumberWithSerenity::class)
class IntegrationTestRunner
