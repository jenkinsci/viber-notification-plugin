package jenkins.plugins.viber_notifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.annotation.Nonnull;


import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

public class ViberNotifier extends Builder {

    private @Nonnull String token;
	private @Nonnull String sender ;
	private @Nonnull String message;
   
	private  String from;
	private  String type="text";
	private String  getAccountInfoUrl = "https://chatapi.viber.com/pa/get_account_info";
	private String  postMessageUrl="https://chatapi.viber.com/pa/post";

	
	@DataBoundConstructor
	public ViberNotifier(@Nonnull String token,@Nonnull String sender, @Nonnull String message) {
		this.token = token;
		this.sender=sender;
		this.message=message;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	@Nonnull
	public String getToken() {
		return token;
	}

	@DataBoundSetter
	public void setToken(String token) {
		this.token = token;
	}
	
	
	@Nonnull
	 public String getSender() {
		return sender;
	}

	@DataBoundSetter
	public void setSender(String sender) {
		this.sender = sender;
	}

	@Nonnull
	public String getMessage() {
		return message;
	}

	@DataBoundSetter
	public void setMessage(String message) {
		this.message = message;
	}


	@Override
	    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener)
	    throws InterruptedException, IOException{
		
			setFrom(getViberAccountMemInfo());
			int buildNumber = build.getNumber();
			String buildName = build.getFullDisplayName();
			postMessageToViber(build, formPayload(buildNumber, buildName));
		 
			return true;
	 }
	
	
	public String formPayload(int buildNumber, String buildName) {
	
		String requestBody;
		JSONObject json = new JSONObject();
		json.put("from",  getFrom());
		JSONObject item = new JSONObject();
		item.put("name", getSender());
		json.put("sender", item);
		json.put("type", type);
		json.put("text", buildName + " " + getMessage());
		requestBody = json.toString();
		return requestBody;
	}
	
	
		public  String getViberAccountMemInfo(){
		
		StringBuffer jsonString = new StringBuffer();
		String adminId = null;
		try {
			URL url = new URL(getAccountInfoUrl);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("X-Viber-Auth-Token", getToken());
			connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					connection.getInputStream(), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				jsonString.append(line);
			}
			br.close();
			connection.disconnect();
			JSONObject obj = new JSONObject(jsonString.toString());
			JSONArray arr = obj.getJSONArray("members");
			for (int i = 0; i < arr.length(); i++)
			{
				if(arr.getJSONObject(i).getString("role").equals("admin")){
					 adminId = arr.getJSONObject(i).getString("id");
				}
			}	
		} catch (Exception e) {
			throw new RuntimeException();
		}
		return adminId;	
	}

	public  String postMessageToViber(Run<?, ?> build, String payload) {
		StringBuffer jsonString = new StringBuffer();
		try {
			URL url = new URL(postMessageUrl);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("X-Viber-Auth-Token",getToken());
			connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");
			OutputStreamWriter writer = new OutputStreamWriter(
					connection.getOutputStream(), "UTF-8");
			writer.write(payload);
			writer.close();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					connection.getInputStream(), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				jsonString.append(line);
			}
			br.close();
			connection.disconnect();
		} catch (Exception e) {
			throw new RuntimeException();
		}
		return jsonString.toString();
	}
	
	@Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
        public DescriptorImpl() {
            load();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Viber Notification";
        } 
        
       
	}
    
	
	
	

}
