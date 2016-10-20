/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.environment.server;

import org.eclipse.che.api.environment.server.compose.ComposeEnvironmentImpl;
import org.eclipse.che.api.environment.server.compose.ComposeFileParser;
import org.eclipse.che.api.environment.server.compose.ComposeServiceImpl;
import org.mockito.InjectMocks;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test for {@link ComposeFileParser}.
 *
 * @author Alexander Andrienko
 */
@Listeners(MockitoTestNGListener.class)
public class ComposeFileParserTest {

    @InjectMocks
    private ComposeFileParser composeFileParser;

    private static final String RECIPE_WITHOUT_COMMAND_VALUE = "services:\n" +
                                                               " machine1:\n" +
                                                               "  image: codenvy/mysql\n" +
                                                               "  environment:\n" +
                                                               "   MYSQL_USER: petclinic\n" +
                                                               "   MYSQL_PASSWORD: password\n" + "  mem_limit: 2147483648\n" +
                                                               "  command: %s\n" + //<- test target
                                                               "  expose: [4403, 5502]";

    @Test(dataProvider = "validCommand")
    public void composeServiceCommandShouldBeParsedSuccessfully(String command,
                                                                List<String> commandWords,
                                                                int commandNumberOfWords) throws Exception {
        String recipe = format(RECIPE_WITHOUT_COMMAND_VALUE, command);
        ComposeEnvironmentImpl composeEnvironment = composeFileParser.parse(recipe, "text/x-yaml");

        assertEquals(composeEnvironment.getServices().size(), 1);
        ComposeServiceImpl service = composeEnvironment.getServices().get("machine1");
        assertEquals(service.getImage(), "codenvy/mysql");
        assertEquals(service.getMemLimit().longValue(), 2147483648L);
        Map<String, String> environment = service.getEnvironment();
        assertEquals(environment.size(), 2);
        assertEquals(environment.get("MYSQL_USER"), "petclinic");
        assertEquals(environment.get("MYSQL_PASSWORD"), "password");
        assertTrue(service.getExpose().containsAll(asList("4403", "5502")));

        assertTrue(service.getCommand().containsAll(commandWords));
        assertEquals(service.getCommand().size(), commandNumberOfWords);
    }

    @DataProvider(name = "validCommand")
    private Object[][] validCommand() {
        return new Object[][] {
                //allow command in one line
                {"service mysql start", asList("service", "mysql", "start"), 3},
                {"service mysql              start", asList("service", "mysql", "start"), 3},
                {"service mysql start         ", asList("service", "mysql", "start"), 3},
                {"service mysql start         ", asList("service", "mysql", "start"), 3},

                //allow break line feature
                {"| \n" +
                 "   service mysql\n" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"| \r" +
                 "   service mysql\r" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"| \r\n" +
                 "   service mysql\r\n" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"| \n\n" +
                 "   service mysql\n\n" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"| \n \n" +
                 "   service mysql\n \n" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"> \n \n" +
                 "   service mysql\n \n" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"> \n \n" +
                 "   ls -a\n \n" +
                 "   -i -p", asList("ls", "-a", "-i", "-p"), 4},

                //allow list command words
                    //first form
                {"[service, mysql, start]", asList("service", "mysql", "start"), 3},
                    //second form
                {"\n" +
                 "   - tail\n" +
                 "   - -f\n" +
                 "   - /dev/null", asList("tail", "-f", "/dev/null"), 3},

                //Some special symbol should be accessible in case line was wrapped by quotes
                {"\"echo ${PWD}\"", asList("echo", "${PWD}"), 2},
                {"\"(Test)\"", singletonList("(Test)"), 1},

                {"", singletonList(""), 1},
        };
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          dataProvider = "inValidCommand")
    public void composeServiceCommandShouldBeParsedFailed(String command) throws Exception {//, String errorMessage
        String recipe = format(RECIPE_WITHOUT_COMMAND_VALUE, command);
        try {
            composeFileParser.parse(recipe, "text/x-yaml");
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            throw e;
        }
    }

    @DataProvider(name = "inValidCommand")
    private Object[][] inValidCommand() {
        return new Object[][] {
                {"{service mysql start}"},
                {"service mysql \nstart"},
                {"test : value"}
        };
    }
}
