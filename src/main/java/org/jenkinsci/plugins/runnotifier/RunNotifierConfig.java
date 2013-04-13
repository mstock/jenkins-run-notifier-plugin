package org.jenkinsci.plugins.runnotifier;

import hudson.Extension;
import hudson.util.FormValidation;

import java.net.URI;
import java.net.URISyntaxException;

import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Configuration for the run notifier plugin.
 */
@Extension
public class RunNotifierConfig extends GlobalConfiguration {
	private String uri;

	/**
	 * Load configuration.
	 */
	public RunNotifierConfig() {
		super();
		load();
	}

	/**
	 * Update configuration and save it to disk.
	 */
	@Override
	public boolean configure(StaplerRequest req, JSONObject formData)
			throws FormException {
		uri = formData.getString("uri");
		save();
		return super.configure(req, formData);
	}

	/**
	 * Validate the URI value.
	 *
	 * @param uri
	 *            The URI for the run notifier.
	 * @return OK on success, an error otherwise.
	 */
	public FormValidation doCheckUri(@QueryParameter String uri) {
		if (uri != null && uri != "") {
			try {
				URI uriObj = new URI(uri);
				String scheme = uriObj.getScheme();
				if (scheme == null
						|| (!scheme.equalsIgnoreCase("http") && !scheme
								.equalsIgnoreCase("https"))) {
					return FormValidation
							.error("Only http:// and https:// URIs supported");
				}
				if (uriObj.getHost() == null) {
					return FormValidation
							.error("URI must contain a host component");
				}
			} catch (URISyntaxException e) {
				return FormValidation.error("Invalid URI format: "
						+ e.getMessage());
			}
		}
		return FormValidation.ok();
	}

	/**
	 * Setter for the URI.
	 *
	 * @param uri
	 *            The new URI value.
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * Getter for the URI.
	 *
	 * @return the current URI value.
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Method to get the configuration instance.
	 *
	 * @return the configuration.
	 */
	public static RunNotifierConfig get() {
		return GlobalConfiguration.all().get(RunNotifierConfig.class);
	}
}
