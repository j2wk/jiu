package bglutil.jiu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import com.oracle.bmc.core.Compute;
import com.oracle.bmc.core.VirtualNetwork;
import com.oracle.bmc.core.model.AttachIScsiVolumeDetails;
import com.oracle.bmc.core.model.IScsiVolumeAttachment;
import com.oracle.bmc.core.model.Image;
import com.oracle.bmc.core.model.Instance;
import com.oracle.bmc.core.model.LaunchInstanceDetails;
import com.oracle.bmc.core.model.Shape;
import com.oracle.bmc.core.model.Vnic;
import com.oracle.bmc.core.model.VnicAttachment;
import com.oracle.bmc.core.model.VolumeAttachment;
import com.oracle.bmc.core.requests.AttachVolumeRequest;
import com.oracle.bmc.core.requests.DetachVolumeRequest;
import com.oracle.bmc.core.requests.GetImageRequest;
import com.oracle.bmc.core.requests.GetInstanceRequest;
import com.oracle.bmc.core.requests.GetVnicRequest;
import com.oracle.bmc.core.requests.GetVolumeAttachmentRequest;
import com.oracle.bmc.core.requests.LaunchInstanceRequest;
import com.oracle.bmc.core.requests.ListImagesRequest;
import com.oracle.bmc.core.requests.ListInstancesRequest;
import com.oracle.bmc.core.requests.ListShapesRequest;
import com.oracle.bmc.core.requests.ListVnicAttachmentsRequest;
import com.oracle.bmc.core.requests.ListVolumeAttachmentsRequest;
import com.oracle.bmc.core.requests.TerminateInstanceRequest;
import com.oracle.bmc.core.responses.GetInstanceResponse;
import com.oracle.bmc.core.responses.LaunchInstanceResponse;

import bglutil.jiu.common.Helper;
import bglutil.jiu.common.UtilMain;

/**
 * Compute service utilities.
 * @author bgl
 *
 */
public class UtilCompute extends UtilMain{
	public UtilCompute() {
		super();
	}
	
	/**
	 * Get volume attachment information in a compartment.
	 * @param c
	 * @param i
	 * @param compartmentId
	 * @return
	 */
	public List<VolumeAttachment> getVolumeAttachment(Compute c, Instance i, String compartmentId){
		return c.listVolumeAttachments(ListVolumeAttachmentsRequest.builder().instanceId(i.getId())
				.availabilityDomain(i.getAvailabilityDomain())
				.compartmentId(compartmentId).build()).getItems();
	}
	
	/**
	 * Get private ip addresses on VM instance.
	 * @param c
	 * @param vn
	 * @param instanceId
	 * @param compartmentId
	 * @return
	 */
	public List<String> getPrivateIpByInstanceId(Compute c, VirtualNetwork vn, String instanceId, String compartmentId){
		List<String> ips = new ArrayList<String>();
		List<Vnic> atchs = this.getVnicByInstanceId(c, vn, instanceId, compartmentId);
		for(Vnic v:atchs){
			ips.add(v.getPrivateIp());
		}
		return ips;
	}
	
	/**
	 * Get VNICs on VM instance.
	 * @param c
	 * @param vn
	 * @param instanceId
	 * @param compartmentId
	 * @return
	 */
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
	
	/**
	 * Instance Name to Instance ID.
	 * @param c
	 * @param name
	 * @param compartmentId
	 * @return
	 */
	public String getInstanceIdByName(Compute c, String name, String compartmentId){
		List<Instance> instances = c.listInstances(ListInstancesRequest.builder().compartmentId(compartmentId).build()).getItems();
		String ocid = null;
		for(Instance i:instances){
			if(i.getDisplayName().equals(name) && !i.getLifecycleState().equals(Instance.LifecycleState.Terminated)){
				ocid = i.getId();
			}
		}
		return ocid;
	}
	
	/**
	 * Get all instances in a compartment.
	 * @param c
	 * @param compartmentId
	 * @return
	 */
	public List<Instance> getAllInstance(Compute c, String compartmentId){
		List<Instance> instances = c.listInstances(ListInstancesRequest.builder().compartmentId(compartmentId).build()).getItems();
		return instances;
	}
	
	/**
	 * Get instance java object by name searching.
	 * @param c
	 * @param name
	 * @param compartmentId
	 * @return
	 */
	public Instance getInstanceByName(Compute c, String name, String compartmentId){
		List<Instance> instances = c.listInstances(ListInstancesRequest.builder().compartmentId(compartmentId).build()).getItems();
		Instance inst = null;
		for(Instance i:instances){
			if(i.getDisplayName().equals(name) && !i.getLifecycleState().equals(Instance.LifecycleState.Terminated)){
				inst = i;
			}
		}
		return inst;
	}
	
	/**
	 * Get instance private/public ip address by name searching.
	 * @param c
	 * @param vn
	 * @param name
	 * @param privateOrPublic
	 * @param compartmentId
	 * @return
	 */
	public String getInstanceIpByName(Compute c, VirtualNetwork vn, String name, String privateOrPublic, String compartmentId){
		String instanceId = this.getInstanceIdByName(c, name, compartmentId);
		List<Vnic> vnics = this.getVnicByInstanceId(c, vn, instanceId, compartmentId);
		String ret = null;
		for(Vnic v:vnics){
			if(privateOrPublic.equals("private")){
				ret = v.getPrivateIp();
				break;
			}
			else if(privateOrPublic.equals("public")){
				ret = v.getPublicIp();
				break;
			}
		}
		return ret;
	}
	
	/**
	 * Instance ID to Instance Name.
	 * @param c
	 * @param instanceId
	 * @return
	 */
	public String getInstanceNameById(Compute c, String instanceId){
		return c.getInstance(GetInstanceRequest.builder().instanceId(instanceId).build()).getInstance().getDisplayName();
	}
	
	/**
	 * Image ID to Image Name.
	 * @param c
	 * @param imageId
	 * @return
	 */
	public String getImageNameById(Compute c, String imageId){
		return c.getImage(GetImageRequest.builder().imageId(imageId).build()).getImage().getDisplayName();
	}
	
	/**
	 * Image Name to Image ID.
	 * @param c
	 * @param imageName
	 * @param compartmentId
	 * @return
	 */
	public String getImageIdByName(Compute c, String imageName, String compartmentId){
		List<Image> images = this.getAllImage(c, compartmentId);
		String ret = null;
		for(Image i:images){
			if(i.getDisplayName().equals(imageName)){
				ret = i.getId();
				break;
			}
		}
		return ret;
	}
	
	/**
	 * Get all available images.
	 * @param c
	 * @param compartmentId
	 * @return
	 */
	public List<Image> getAllImage(Compute c, String compartmentId){
		return c.listImages(ListImagesRequest.builder().compartmentId(compartmentId).build()).getItems();
	}
	
	/**
	 * Get all available shapes.
	 * @param c
	 * @param compartmentId
	 * @return
	 */
	public Set<String> getAllShape(Compute c, String compartmentId){
		TreeSet<String> ts = new TreeSet<String>();
		for(Shape s:c.listShapes(ListShapesRequest.builder().compartmentId(compartmentId).build()).getItems()){
			String sh = s.getShape();
			String ss = null;
			if(sh.equals("BM.Standard1.36")){
				ss = sh+" 256GB";
			}
			else if(sh.equals("BM.HighIO1.36")){
				ss = sh+" 512GB 12.8TB NVMe SSD";
			}
			else if(sh.equals("BM.DenseIO1.36")){
				ss = sh+" 512GB 28.8TB NVMe SSD";
			}
			else if(sh.equals("VM.Standard1.1")){
				ss = sh+" 7GB";
			}
			else if(sh.equals("VM.Standard1.2")){
				ss = sh+" 14GB";
			}
			else if(sh.equals("VM.Standard1.4")){
				ss = sh+" 28GB";
			}
			else if(sh.equals("VM.Standard1.8")){
				ss = sh+" 56GB";
			}
			else if(sh.equals("VM.Standard1.16")){
				ss = sh+" 112GB";
			}
			else if(sh.equals("VM.DenseIO1.4")){
				ss = sh+" 60GB 3.2TB NVMe SSD";
			}
			else if(sh.equals("VM.DenseIO1.8")){
				ss = sh+" 120GB 6.4TB NVMe SSD";
			}
			else if(sh.equals("VM.DenseIO1.16")){
				ss = sh+" 240GB 12.8TB NVMe SSD";
			}
			ts.add(ss);
		}
		return ts;
	}
	
	/**
	 * Terminate Vm instance.
	 * @param c
	 * @param compartmentId
	 * @param instanceId
	 * @throws Exception
	 */
	public void killInstanceById(Compute c, String compartmentId, String instanceId) throws Exception{
		String name = this.getInstanceNameById(c, instanceId);
		c.terminateInstance(TerminateInstanceRequest.builder().instanceId(instanceId).build());
		h.waitForInstanceStatus(c, instanceId, Instance.LifecycleState.Terminating, "Terminating VM Instance "+name, true);
	}
	
	/**
	 * Detach volume from instance.
	 * @param c
	 * @param volumeAttachmentId
	 */
	public void detachVolume(Compute c, String volumeAttachmentId){
		c.detachVolume(DetachVolumeRequest.builder().volumeAttachmentId(volumeAttachmentId).build());
		VolumeAttachment check = null;
		while(true){
			Helper.wait(1000);
			check = c.getVolumeAttachment(GetVolumeAttachmentRequest.builder().volumeAttachmentId(volumeAttachmentId).build()).getVolumeAttachment();
			sk.printResult(0,true,check.getLifecycleState().getValue());
			if(check.getLifecycleState().equals(VolumeAttachment.LifecycleState.Detached)){
				break;
			}
		}
	}
	
	/**
	 * Attach iSCSI volume to instance.
	 * @param c
	 * @param attachName
	 * @param instanceId
	 * @param volumeId
	 * @return
	 */
	public IScsiVolumeAttachment attachVolumeIscsi(Compute c, String attachName, String instanceId, String volumeId){
		IScsiVolumeAttachment va = (IScsiVolumeAttachment) c.attachVolume(AttachVolumeRequest.builder().attachVolumeDetails(AttachIScsiVolumeDetails.builder()
				.displayName(attachName)
				.instanceId(instanceId)
				.volumeId(volumeId).build()
				).build()).getVolumeAttachment();
		return va;
	}
	
	/**
	 * Launch a new instance.
	 * @param c
	 * @param compartmentId
	 * @param subnetId
	 * @param name
	 * @param imageId
	 * @param shapeId
	 * @param sshPublicKey
	 * @param ad
	 * @param userdataBase64
	 * @param targetState
	 * @return
	 * @throws Exception
	 */
	public GetInstanceResponse createInstance(Compute c, String compartmentId, String subnetId, String name, String imageId, String shapeId, String sshPublicKey, String ad, String userdataBase64, Instance.LifecycleState targetState) throws Exception{
		Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("ssh_authorized_keys", sshPublicKey);
        if(userdataBase64!=null){
        	metadata.put("user_data", userdataBase64);
        }
        
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
