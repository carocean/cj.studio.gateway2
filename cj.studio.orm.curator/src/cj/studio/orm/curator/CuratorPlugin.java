package cj.studio.orm.curator;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.IChipPlugin;
import cj.studio.ecm.context.IElement;
import cj.studio.ecm.context.IProperty;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;

public class CuratorPlugin implements IChipPlugin {
    CuratorFramework client;

    @Override
    public void load(IAssemblyContext ctx, IElement args) {
        String retryIntervalMs = args.getNode("retryIntervalMs") == null ? "5000" : ((IProperty) args.getNode("retryIntervalMs")).getValue().getName();
        String connectString = args.getNode("connectString") == null ? "" : ((IProperty) args.getNode("connectString")).getValue().getName();
        String sessionTimeoutMs = args.getNode("sessionTimeoutMs") == null ? "60000" : ((IProperty) args.getNode("sessionTimeoutMs")).getValue().getName();
        String connectionTimeoutMs = args.getNode("connectionTimeoutMs") == null ? "60000" : ((IProperty) args.getNode("connectionTimeoutMs")).getValue().getName();
        String namespace = args.getNode("namespace") == null ? "" : ((IProperty) args.getNode("namespace")).getValue().getName();
        RetryPolicy retryPolicy = new RetryForever(Integer.valueOf(retryIntervalMs));
        client =
                CuratorFrameworkFactory.builder()
                        .connectString(connectString)
                        .sessionTimeoutMs(Integer.valueOf(sessionTimeoutMs))
                        .connectionTimeoutMs(Integer.valueOf(connectionTimeoutMs))
                        .retryPolicy(retryPolicy)
                        .namespace(namespace)
                        .build();
        client.start();
        CJSystem.logging().info(getClass(), String.format("Curator启动成功。connectString=%s, sessionTimeoutMs=%s, connectionTimeoutMs=%s, retryIntervalMs=%s, namespace=%s", connectString, sessionTimeoutMs, connectionTimeoutMs, retryIntervalMs, namespace));
    }

    @Override
    public void unload() {
        client.close();
    }

    @Override
    public Object getService(String serviceId) {
        if ("framework".equals(serviceId)) {
            return client;
        }
        return new EcmException("格式错误，应用：插件名.framework ，例：curator.framework");
    }
}
