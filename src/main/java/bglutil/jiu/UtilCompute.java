package bglutil.jiu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import com.oracle.bmc.core.Compute;
import com.oracle.bmc.core.VirtualNetwork;
import com.oracle.bmc.core.model.Image;
import com.oracle.bmc.core.model.Instance;
import com.oracle.bmc.core.model.LaunchInstanceDetails;
import com.oracle.bmc.core.model.Shape;
import com.oracle.bmc.core.model.Vnic;
import com.oracle.bmc.core.model.VnicAttachment;
import com.oracle.bmc.core.requests.GetInstanceRequest;
import com.oracle.bmc.core.requests.GetVnicRequest;
import com.oracle.bmc.core.requests.GetVolumeAttachmentRequest;
import com.oracle.bmc.core.requests.LaunchInstanceRequest;
import com.oracle.bmc.core.requests.ListImagesRequest;
import com.oracle.bmc.core.requests.ListShapesRequest;
import com.oracle.bmc.core.requests.ListVnicAttachmentsRequest;
import com.oracle.bmc.core.requests.TerminateInstanceRequest;
import com.oracle.bmc.core.responses.GetInstanceResponse;
import com.oracle.bmc.core.responses.LaunchInstanceResponse;


import bglutil.jiu.common.UtilMain;

public class UtilCompute extends UtilMain{
	public UtilCompute() {
		super();
	}
	
	// GETTER //
	
	public List<Vnic> getVnicByInstanceId(Compute c, VirtualNetwork vn, String instanceId, String compartmentId){
		List<VnicAttachment> atchs = c.listVnicAttachments(ListVnicAttachmentsRequest.builder().compartmentId(compartmentId).instanceId(instanceId).build()).getItems();
		List<Vnic> vnics = new ArrayList<Vnic>();
		for(VnicAttachment va: atchs){
			String vid = va.getVnicId();
			Vnic v = vn.getVnic(GetVnicRequest.builder().vnicId(vid).build()).getVnic();
			vnics.add(v);
		}
		return vnics;
	}
	
	public String getInstanceNameById(Compute c, String instanceId){
		return c.getInstance(GetInstanceRequest.builder().instanceId(instanceId).build()).getInstance().getDisplayName();
	}
	
	public List<Image> getAllImage(Compute c, String compartmentId){
		return c.listImages(ListImagesRequest.builder().compartmentId(compartmentId).build()).getItems();
	}
	
	public Set<String> getAllShape(Compute c, String compartmentId){
		TreeSet<String> ts = new TreeSet<String>();
		for(Shape s:c.listShapes(ListShapesRequest.builder().compartmentId(compartmentId).build()).getItems()){
			ts.add(s.getShape());
		}
		return ts;
	}
	
	// KILLER //
	
	public void killInstanceById(Compute c, String compartmentId, String instanceId) throws Exception{
		String name = this.getInstanceNameById(c, instanceId);
		c.terminateInstance(TerminateInstanceRequest.builder().instanceId(instanceId).build());
		h.waitForInstanceStatus(c, instanceId, Instance.LifecycleState.Terminated, "Terminating VM Instance "+name, true);
	}
	
	// CREATOR //
	
	public GetInstanceResponse createVmInstance(Compute c, String compartmentId, String subnetId, String name, String imageId, String shapeId, String sshPublicKey, String ad, Instance.LifecycleState targetState) throws Exception{
		Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("ssh_authorized_keys", sshPublicKey);
        
        LaunchInstanceResponse response =
                c.launchInstance(
                        LaunchInstanceRequest.builder()
                                .launchInstanceDetails(
                                        LaunchInstanceDetails.builder()
                                                .availabilityDomain(ad)
                                                .compartmentId(compartmentId)
                                                .displayName(name)
                                                .imageId(imageId)
                                                .metadata(metadata)
                                                .shape(shapeId)
                                                .subnetId(subnetId)
                                                .build())
                                .build());
        String instanceId = response.getInstance().getId();
        GetInstanceResponse res = h.waitForInstanceStatus(c, instanceId, targetState, targetState.name()+" Instance - "+name, false);
        return res;
	}
	
}
