package org.apache.gossip;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.gossip.event.GossipListener;
import org.apache.gossip.event.GossipState;
import org.apache.gossip.model.GossipDataMessage;
import org.apache.gossip.model.SharedGossipDataMessage;
import org.junit.Test;

import io.teknek.tunit.TUnit;

public class DataTest {
  
  @Test
  public void dataTest() throws InterruptedException, UnknownHostException, URISyntaxException{
    GossipSettings settings = new GossipSettings();
    String cluster = UUID.randomUUID().toString();
    int seedNodes = 1;
    List<GossipMember> startupMembers = new ArrayList<>();
    for (int i = 1; i < seedNodes+1; ++i) {
      URI uri = new URI("udp://" + "127.0.0.1" + ":" + (50000 + i));
      startupMembers.add(new RemoteGossipMember(cluster, uri, i + ""));
    }
    final List<GossipService> clients = new ArrayList<>();
    final int clusterMembers = 2;
    for (int i = 1; i < clusterMembers+1; ++i) {
      URI uri = new URI("udp://" + "127.0.0.1" + ":" + (50000 + i));
      GossipService gossipService = new GossipService(cluster, uri, i + "",
              startupMembers, settings,
              new GossipListener(){
        public void gossipEvent(GossipMember member, GossipState state) {
          
        }
      });
      clients.add(gossipService);
      gossipService.start();
    }
    TUnit.assertThat(new Callable<Integer> (){
      public Integer call() throws Exception {
        int total = 0;
        for (int i = 0; i < clusterMembers; ++i) {
          total += clients.get(i).getGossipManager().getLiveMembers().size();
        }
        return total;
      }}).afterWaitingAtMost(20, TimeUnit.SECONDS).isEqualTo(2);
    clients.get(0).gossipPerNodeData(msg());
    clients.get(0).gossipSharedData(sharedMsg());
    Thread.sleep(10000);
    TUnit.assertThat(
            new Callable<Object>() {
              public Object call() throws Exception {
                GossipDataMessage x = clients.get(1).findPerNodeData(1 + "", "a");
                if (x == null)
                  return "";
                else
                  return x.getPayload();
              }
            }).afterWaitingAtMost(20, TimeUnit.SECONDS).isEqualTo("b");
    
    
    TUnit.assertThat(
            new Callable<Object>() {
              public Object call() throws Exception {
                SharedGossipDataMessage x = clients.get(1).findSharedData("a");
                if (x == null)
                  return "";
                else
                  return x.getPayload();
              }
            }).afterWaitingAtMost(20, TimeUnit.SECONDS).isEqualTo("c");
    
    
    for (int i = 0; i < clusterMembers; ++i) {
      clients.get(i).shutdown();
    }
  }
  
  private GossipDataMessage msg(){
    GossipDataMessage g = new GossipDataMessage();
    g.setExpireAt(Long.MAX_VALUE);
    g.setKey("a");
    g.setPayload("b");
    g.setTimestamp(System.currentTimeMillis());
    return g;
  }
  
  private SharedGossipDataMessage sharedMsg(){
    SharedGossipDataMessage g = new SharedGossipDataMessage();
    g.setExpireAt(Long.MAX_VALUE);
    g.setKey("a");
    g.setPayload("c");
    g.setTimestamp(System.currentTimeMillis());
    return g;
  }
  
}
