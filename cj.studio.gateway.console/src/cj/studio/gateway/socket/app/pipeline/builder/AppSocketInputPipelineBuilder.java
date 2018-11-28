package cj.studio.gateway.socket.app.pipeline.builder;

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
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.logging.ILogging;
import cj.studio.gateway.socket.app.IGatewayAppSiteProgram;
import cj.studio.gateway.socket.app.ProgramAdapterType;
import cj.studio.gateway.socket.app.valve.CheckErrorInputVavle;
import cj.studio.gateway.socket.app.valve.CheckSessionInputValve;
import cj.studio.gateway.socket.app.valve.CheckUrlInputValve;
import cj.studio.gateway.socket.app.valve.FirstJeeInputValve;
import cj.studio.gateway.socket.app.valve.FirstWayInputValve;
import cj.studio.gateway.socket.app.valve.LastJeeInputValve;
import cj.studio.gateway.socket.app.valve.LastWayInputValve;
import cj.studio.gateway.socket.pipeline.IAnnotationInputValve;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.InputPipeline;
import cj.studio.gateway.socket.util.SocketContants;

public class AppSocketInputPipelineBuilder implements IInputPipelineBuilder {
	IServiceProvider parent;
	private Map<String, Object> props;
	private String name;
	static ILogging logger=CJSystem.logging();
	public AppSocketInputPipelineBuilder(IServiceProvider parent) {
		this.parent = parent;
	}

	@Override
	public IInputPipelineBuilder name(String name) {
		this.name=name;
		return this;
	}
	@Override
	public IInputPipeline createPipeline() throws CircuitException {
		IGatewayAppSiteProgram prog = ((IGatewayAppSiteProgram) parent.getService("$.app.program"));
		IInputPipeline input = createInputPipeline(prog);
		String socketName=(String)parent.getService("$.socket.name");
		input.prop(SocketContants.__pipeline_name,name);
		input.prop(SocketContants.__pipeline_toWho,socketName);
		input.prop(SocketContants.__pipeline_toProtocol,"app");
		if(props!=null) {
			Set<String> set=props.keySet();
			for(String key:set) {
				input.prop(key, (String)props.get(key));
			}
		}
		return input;
	}

	protected IInputPipeline createInputPipeline(IServiceProvider app) {
		ProgramAdapterType type=(ProgramAdapterType)app.getService("$.app.type");
		IInputPipeline input = null;
		if (type == ProgramAdapterType.jee) {
			FirstJeeInputValve first=new FirstJeeInputValve();
			LastJeeInputValve last=new LastJeeInputValve();
			
			input=new InputPipeline(first, last);
		} else {
			//注意顺序
			FirstWayInputValve first = new FirstWayInputValve();
			LastWayInputValve last = new LastWayInputValve(app);
			input = new InputPipeline(first, last);
			CheckUrlInputValve checkuri = new CheckUrlInputValve();
			if (checkuri != null) {
				input.add(checkuri);
			}
			
			CheckErrorInputVavle error=new CheckErrorInputVavle(app);
			input.add(error);//添在此位置,error将产生会话，如果不想产生会话可放在sessionValve后面
			
			CheckSessionInputValve session = new CheckSessionInputValve(parent,app);
			input.add(session);
			
		}
		ServiceCollection<IAnnotationInputValve> col=app.getServices(IAnnotationInputValve.class);
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
					logger.warn(getClass(),"必须声明为多例服务，该Valve已被忽略:"+v);
					continue;
				}
				input.add(v);
			}
		}
		return input;
	}

	@Override
	public IInputPipelineBuilder prop(String name, Object value) {
		if(props==null) {
			props=new HashMap<>();
		}
		props.put(name, value);
		return this;
	}

}
