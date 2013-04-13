package org.jenkinsci.plugins.runnotifier;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.Gson;

/**
 * Plug-in for Jenkins which sends HTTP-based notifications at various stages of
 * builds.
 */
@SuppressWarnings("rawtypes")
@Extension
public class RunNotifierListener extends RunListener<Run> {
	private static final Logger LOGGER = Logger
			.getLogger(RunNotifierListener.class.getName());
	private transient final ScheduledExecutorService executor = Executors
			.newSingleThreadScheduledExecutor();

	@Override
	public void onStarted(Run run, TaskListener taskListener) {
		sendNotificationLater(new RunInfo("started", run), new JobInfo(run), 0);
		super.onStarted(run, taskListener);
	}

	@Override
	public void onCompleted(Run run, TaskListener taskListener) {
		sendNotificationLater(new RunInfo("completed", run), new JobInfo(run),
				1);
		super.onCompleted(run, taskListener);
	}

	@Override
	public void onFinalized(Run run) {
		sendNotificationLater(new RunInfo("finalized", run), new JobInfo(run),
				1);
		super.onFinalized(run);
	}

	/**
	 * Send notification after waiting for about one second. Especially in the
	 * "completed" and "finalized" case, this avoids that the busy executor
	 * count does not match the expected value (since the current run still
	 * occupies an executor at the time of this call).
	 *
	 * @param runInfo
	 *            Information about the current run.
	 * @param jobInfo
	 *            Information about the associated job.
	 * @param seconds
	 *            Number of seconds to wait until notification is sent.
	 */
	private void sendNotificationLater(final RunInfo runInfo,
			final JobInfo jobInfo, final int seconds) {
		executor.schedule(new Runnable() {
			public void run() {
				sendNotification(runInfo, jobInfo);
			}
		}, seconds, TimeUnit.SECONDS);
	}

	/**
	 * Send notification to the configured URI using a HTTP POST request.
	 *
	 * @param runInfo
	 *            Information about the current run.
	 * @param jobInfo
	 *            Information about the associated job.
	 */
	private synchronized void sendNotification(RunInfo runStatus,
			JobInfo jobInfo) {
		String uri = RunNotifierConfig.get().getUri();
		if (uri != null && uri != "") {
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost request = new HttpPost(uri);
			Gson gson = new Gson();
			try {
				StringEntity entity = new StringEntity(
						gson.toJson(new JenkinsStatus(runStatus, jobInfo)));
				entity.setContentType("application/json; charset=utf-8");
				request.setEntity(entity);
				httpclient.execute(request);
			} catch (UnsupportedEncodingException e) {
				LOGGER.info("Unable to encode notification data: "
						+ e.getMessage());
			} catch (ClientProtocolException e) {
				LOGGER.info("Unable to send notification: " + e.getMessage());
			} catch (IOException e) {
				LOGGER.info("Unable to send notification: " + e.getMessage());
			}
		}
	}

	/**
	 * Encapsulate status information for Jenkins, the run and the job a
	 * notification will be sent for.
	 */
	@SuppressWarnings("unused")
	private class JenkinsStatus {
		private int totalExecutors = 0;
		private int busyExecutors = 0;
		private String datetime = dateTime();
		private RunInfo run;
		private JobInfo job;

		public JenkinsStatus(RunInfo runInfo, JobInfo jobInfo) {
			this.run = runInfo;
			this.job = jobInfo;
			Computer[] computers = Hudson.getInstance().getComputers();
			for (Computer computer : computers) {
				if (computer.isOnline()) {
					totalExecutors += computer.getNumExecutors();
					for (Executor executor : computer.getExecutors()) {
						if (executor.isBusy()) {
							busyExecutors++;
						}
					}
				}
			}
		}
	}

	/**
	 * Hold information about the run.
	 */
	@SuppressWarnings("unused")
	private class RunInfo {
		private String buildStatusSummary;
		private String name;
		private long duration;
		private String status;
		private String uri;

		public RunInfo(String status, Run run) {
			this.status = status;
			buildStatusSummary = run.getBuildStatusSummary().message;
			name = run.getDisplayName();
			duration = run.getDuration();
			uri = absoluteUri(run.getUrl());
		}
	}

	/**
	 * Hold information about the job.
	 */
	@SuppressWarnings("unused")
	private class JobInfo {
		private String name;
		private String uri;

		public JobInfo(Run run) {
			name = run.getParent().getDisplayName();
			uri = absoluteUri(run.getParent().getUrl());
		}
	}

	/**
	 * Convert a relative URI to an absolute one using the configured root URI
	 * of Jenkins.
	 *
	 * @param relative
	 *            The relative URI.
	 * @return an absolute URI if the root URI is configured, <code>null</code>
	 *         otherwise.
	 */
	private String absoluteUri(String relative) {
		String uri = Hudson.getInstance().getRootUrl();
		if (uri != null) {
			uri += relative;
		}
		return uri;
	}

	/**
	 * Get current date and time in ISO8601 format.
	 *
	 * @return a string with current date and time.
	 */
	private String dateTime() {
		DateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date();
		return dateFormat.format(date);
	}
}
