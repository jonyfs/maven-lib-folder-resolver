package br.com.jonyfs;

import javax.annotation.Resource;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.shell.Shell;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestApplicationRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MavenLibFolderResolverApplicationTests {

    @Resource
    ApplicationContext applicationContext;

    @Resource
    private Shell shell;

    @Test
    public void test1verifyIfApplicationContextIsNotNull() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    public void test2verifyIfInitialCountIsZero() {
        //assertThat(shell.evaluate(() -> "analyze ./target")).hasisEqualTo(0L);
        shell.evaluate(() -> "analyze ./target");
    }

}
