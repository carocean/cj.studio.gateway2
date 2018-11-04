package cj.studio.gateway.socket.pipeline;


import cj.studio.ecm.graph.CircuitException;
import cj.ultimate.IClosable;

public class OutputPipeline implements IOutputPipeline {
    LinkEntry head;
    LinkEntry last;
    IOPipeline adapter;
    public OutputPipeline(IOutputValve first, IOutputValve last) {
        head=new LinkEntry(first);
        head.next=new LinkEntry(last);
        this.last=head.next;
        this.adapter=new OPipeline(this);
    }


    public void add(IOutputValve valve) {
        LinkEntry entry=getEndConstomerEntry();
        if(entry==null){
            return;
        }
        LinkEntry lastEntry=entry.next;
        entry.next=new LinkEntry(valve);
        entry.next.next=lastEntry;
    }

    private LinkEntry getEndConstomerEntry() {
        if(head==null)return null;
        LinkEntry tmp=head;
        do{
            if(last.equals(tmp.next)){
                return tmp;
            }
            tmp=tmp.next;
        }while (tmp.next!=null);
        return null;
    }

    public void remove(IOutputValve valve) {
        LinkEntry tmp=head;
        do{
            if(valve.equals(tmp.next.entry)){
                break;
            }
            tmp=tmp.next;
        }while (tmp.next!=null);
        tmp.next=tmp.next.next;
    }

    @Override
    public void headFlow(Object request,Object response) throws CircuitException {
        nextFlow(request,response,null);
    }

    @Override
    public void nextFlow(Object request,Object response, IOutputValve formthis) throws CircuitException {
        if(formthis==null){
            head.entry.flow(request,response,this);
            return;
        }
        LinkEntry linkEntry=lookforHead(formthis);
        if(linkEntry==null||linkEntry.next==null)return;
        linkEntry.next.entry.flow(request,response,this);
    }

    private LinkEntry lookforHead(IOutputValve formthis) {
        if(head==null)return null;
        LinkEntry tmp=head;
        do{
            if(formthis.equals(tmp.entry)){
                break;
            }
            tmp=tmp.next;
        }while (tmp.next!=null);
        return tmp;
    }

    public void dispose() {
        LinkEntry tmp=head;
        while(tmp!=null){
            tmp=tmp.next;
            tmp.entry=null;
            tmp.next=null;
        }
    }

    class LinkEntry{
        LinkEntry next;
        IOutputValve entry;

        public LinkEntry(IOutputValve entry) {
            this.entry=entry;
        }

    }

	@Override
	public void close() {
		if(this.last!=null&&last instanceof IClosable) {
			IClosable closable=(IClosable)last;
			closable.close();
		}
	}

	class OPipeline implements IOPipeline{
		IOutputPipeline target;
		public OPipeline(IOutputPipeline target) {
			this.target=target;
		}
		@Override
		public void nextFlow(Object request, Object response, IOutputValve formthis) throws CircuitException {
			target.nextFlow(request, response, formthis);
		}

		@Override
		public void close() throws CircuitException {
			target.close();
		}
		
	}
}