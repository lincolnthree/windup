package org.windup.examples.ejb.messagedriven;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import com.ibm.mq.jms.MQDestination;

@MessageDriven(
         name = "MyNameForMessageDrivenBean",
         activationConfig = {
                  @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/MyQueue")
         })
public class MessageDrivenBean
{

   public void doBadStuff()
   {
      MQDestination dest = new MQDestination();
   }
}
