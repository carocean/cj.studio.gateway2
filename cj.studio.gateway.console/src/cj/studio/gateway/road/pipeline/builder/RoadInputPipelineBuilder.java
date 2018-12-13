package cj.studio.gateway.road.pipeline.builder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.Scope;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.road.valve.FirstRoadInputValve;
import cj.studio.gateway.road.valve.LastRoadInputValve;
import cj.studio.gateway.socket.app.IGatewayAppSiteProgram;
import cj.studio.gateway.socket.pipeline.IAnnotationInputValve;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.InputPipeline;
import cj.studio.gateway.socket.util.SocketContants;
import io.netty.channel.Channel;

public class RoadInputPipelineBuilder implements IInputPipelineBuilder {
	IServiceProvider parent;
	private Channel frontend;
	private Map<String, Object> props;
	private String name;
	public RoadInputPipelineBuilder(IServiceProvider parent) {
		this.parent = parent;
	}

	@Override
	public IInputPipelineBuilder name(String name) {
		this.name=name;
		return this;
	}

	@Override
	public IInputPipelineBuilder prop(String name, Object value) {
		if(value instanceof Channel) {
			frontend=(Channel)value;
			return this;
		}
		if(props==null) {
			props=new HashMap<>();
		}
		props.put(name, value);
		return this;
	}

	@Override
	public IInputPipeline createPipeline() throws CircuitException {
		IGatewayAppSiteProgram prog = ((IGatewayAppSiteProgram) parent.getService("$.app.program"));
		IInputPipeline input = createInputPipeline(prog);
		String socketName=(String)parent.getService("$.socket.name");
		input.prop(SocketContants.__pipeline_name,name);
		input.prop(SocketContants.__pipeline_toWho,socketName);
		input.prop(SocketContants.__pipeline_toProtocol,"road");
		if(props!=null) {
			Set<String> set=props.keySet();
			for(String key:set) {
				input.prop(key, (String)props.get(key));
			}
		}
		return input;
	}

	private IInputPipeline createInputPipeline(IServiceProvider road) {
		//注意顺序
		FirstRoadInputValve first = new FirstRoadInputValve();
		LastRoadInputValve last = new LastRoadInputValve(parent,this.frontend);
		IInputPipeline input = new InputPipeline(first, last);
		
		ServiceCollection<IAnnotationInputValve> col=road.getServices(IAnnotationInputValve.class);
		if(!col.isEmpty()) {
			List<IAnnotationInputValve> list=col.asList();
			IAnnotationInputValve[] arr=list.toArray(new IAnnotationInputValve[0]);
			Arrays.sort(arr, new Comparator<IAnnotationInputValve>() {

				@Override
				public int compare(IAnnotationInputValve o1, IAnnotationInputValve o2) {
					if(o1.getSort()==o2.getSort())return 0;
					return o1.getSort()>o2.getSort()?1:-1;
				}
				
			});
			for(IAnnotationInputValve v:arr) {
				CjService cjService=v.getClass().getDeclaredAnnotation(CjService.class);
				if(cjService.scope()!=Scope.multiton) {
					CJSystem.logging().warn(getClass(),"必须声明为多例服务，该Valve已被忽略:"+v);
					continue;
				}
				input.add(v);
			}
		}
		return input;
	}

}
