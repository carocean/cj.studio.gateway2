package cj.studio.orm.curator;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.IChipPlugin;
import cj.studio.ecm.context.IElement;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;

public class CuratorPlugin implements IChipPlugin {
    CuratorFramework client;

    @Override
    public void load(IAssemblyContext ctx, IElement args) {
        String retryIntervalMs = args.getNode("retryIntervalMs") == null ? "5000" : args.getNode("retryIntervalMs").getName();
        String connectString = args.getNode("connectString") == null ? "" : args.getNode("connectString").getName();
        String sessionTimeoutMs = args.getNode("sessionTimeoutMs") == null ? "60000" : args.getNode("sessionTimeoutMs").getName();
        String connectionTimeoutMs = args.getNode("connectionTimeoutMs") == null ? "60000" : args.getNode("connectionTimeoutMs").getName();
        String namespace = args.getNode("namespace") == null ? "/" : args.getNode("namespace").getName();
        RetryPolicy retryPolicy = new RetryForever(Integer.valueOf(retryIntervalMs));
        CuratorFramework client =
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
        return client;
    }
}
