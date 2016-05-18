package gov.nih.nci.hpc.cli.commands;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.support.DefaultPromptProvider;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HpcPromptProvider extends DefaultPromptProvider {

	@Override
	public String getPrompt() {
		return "hpc-cli>";
	}

	
	@Override
	public String getProviderName() {
		return "Hpc prompt provider";
	}

}
