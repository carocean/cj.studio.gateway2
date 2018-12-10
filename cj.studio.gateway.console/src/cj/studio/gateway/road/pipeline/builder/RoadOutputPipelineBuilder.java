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
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.logging.ILogging;
import cj.studio.gateway.road.valve.FirstRoadOutputValve;
import cj.studio.gateway.road.valve.LastRoadOutputValve;
import cj.studio.gateway.socket.app.IGatewayAppSiteProgram;
import cj.studio.gateway.socket.pipeline.IAnnotationOutputValve;
import cj.studio.gateway.socket.pipeline.IOutputPipeline;
import cj.studio.gateway.socket.pipeline.IOutputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IOutputValve;
import cj.studio.gateway.socket.pipeline.OutputPipeline;
import cj.studio.gateway.socket.util.SocketContants;
import io.netty.channel.Channel;

public class RoadOutputPipelineBuilder implements IOutputPipelineBuilder {
	IServiceProvider parent;
	private Map<String, Object> props;
	private String name;
	private Channel frontend;
	private Channel backend;
	static ILogging logger = CJSystem.logging();

	public RoadOutputPipelineBuilder(IServiceProvider parent) {
		this.parent = parent;
	}

	@Override
	public IOutputPipelineBuilder name(String name) {
		this.name = name;
		return this;
	}

	@Override
	public IOutputPipelineBuilder prop(String name, Object value) {
		if (value instanceof Channel) {
			if (SocketContants.__pipeline_builder_frontend_channel.equals(name)) {
				frontend = (Channel) value;
			} else {
				backend = (Channel) value;
			}
			return this;
		}
		if (props == null) {
			props = new HashMap<>();
		}
		props.put(name, value);
		return this;
	}

	@Override
	public IOutputPipeline createPipeline() throws CircuitException {
		IGatewayAppSiteProgram prog = ((IGatewayAppSiteProgram) parent.getService("$.app.program"));
		IOutputPipeline output = createOutputPipeline(prog);
		if (props != null) {
			Set<String> set = props.keySet();
			for (String key : set) {
				output.prop(key, (String) props.get(key));
			}
		}
		if (output == null) {
			throw new CircuitException("404", "road socket 无法创建输出管道：" + prog);
		}
		return output;
	}

	private IOutputPipeline createOutputPipeline(IServiceProvider app) {
		IOutputValve first = new FirstRoadOutputValve();
		IOutputValve last = new LastRoadOutputValve(this.parent, frontend, backend);
		IOutputPipeline output = new OutputPipeline(first, last);
		if (props != null) {
			Set<String> set = props.keySet();
			for (String key : set) {
				output.prop(key, (String) props.get(key));
			}
		}
		String socketName = (String) parent.getService("$.socket.name");
		output.prop(SocketContants.__pipeline_name, name);
		output.prop(SocketContants.__pipeline_fromWho, socketName);
		output.prop(SocketContants.__pipeline_fromProtocol, "road");

		ServiceCollection<IAnnotationOutputValve> col = app.getServices(IAnnotationOutputValve.class);
		if (!col.isEmpty()) {
			List<IAnnotationOutputValve> list = col.asList();
			IAnnotationOutputValve[] arr = list.toArray(new IAnnotationOutputValve[0]);
			Arrays.sort(arr, new Comparator<IAnnotationOutputValve>() {

				@Override
				public int compare(IAnnotationOutputValve o1, IAnnotationOutputValve o2) {
					if (o1.getSort() == o2.getSort())
						return 0;
					return o1.getSort() > o2.getSort() ? 1 : -1;
				}

			});
			for (IAnnotationOutputValve v : arr) {
				CjService cjService = v.getClass().getDeclaredAnnotation(CjService.class);
				if (cjService.scope() != Scope.multiton) {
					logger.warn(getClass(), "必须声明为多例服务，该Valve已被忽略:" + v);
					continue;
				}
				output.add(v);
			}
		}

		return output;
	}


}
