package org.vrjuggler.jccl.editors;

import java.awt.Color;
import java.util.List;
import javax.swing.*;

import info.clearthought.layout.*;
import org.vrjuggler.jccl.config.*;
import org.vrjuggler.jccl.config.event.*;

/**
 * Provides a panel that contains a variable number of property values.
 */
public class VarListPanel extends ExpandablePanel implements ConfigElementListener
{
   public void finalize()
   {
      mConfigElement.removeConfigElementListener(this);
      super.finalize();
   }
   
   /**
    * Construct a panel to hold the sheet that actually contains the list of
    * properties.
    */
   public VarListPanel(ConfigElement elm, PropertyDefinition prop_def, Color color)
   {
      super(color);

      mConfigElement = elm;
   
      mConfigElement.addConfigElementListener(this);
            
      // Create the new property list which is actually just a variable list of
      // property values.
      PropertySheet new_sheet = PropertySheetFactory.instance().makeVarSheet(mConfigElement, prop_def, getNextColor());

      mComponent = new_sheet;
      mPropDef = prop_def;
      new_sheet.setBorder(BorderFactory.createEtchedBorder());

      setBorder(BorderFactory.createEtchedBorder());

      // Add title for the list.
      String title = prop_def.getToken();
      JLabel name = new JLabel(title);
      TableLayoutConstraints c2 = new TableLayoutConstraints(2, 0, 2, 0, TableLayout.LEFT, TableLayout.TOP);
      add(name, c2);
   }

   /**
    * A property value was added so we must show this additional property.
    */
   public void propertyValueAdded(ConfigElementEvent evt)
   {
      // XXX: We could also test against mConfigElement == elm
      ConfigElement elm = (ConfigElement)evt.getSource();
      PropertyDefinition prop_def =
         elm.getDefinition().getPropertyDefinition(evt.getProperty());
      
      if(!prop_def.equals(mPropDef))
      {
         return;      
      }
      
      System.out.println("Property value added...");
      
      if(ConfigElement.class == prop_def.getType())
      {
         // Use the PropertySheetFactory to add an additional embedded element.
         PropertySheetFactory.instance().addEmbeddedElement((PropertySheet)mComponent, elm, evt.getValue(), prop_def, 2);
      }
      else
      {
         // Use the PropertySheetFactory to add an aditional normal editor.
         String label = prop_def.getPropertyValueDefinition(0).getLabel();
         PropertySheetFactory.instance().addNormalEditor((PropertySheet)mComponent, elm, 
                                                         evt.getValue(), prop_def, label, 2,
                                                         elm.getPropertyValueCount(prop_def.getToken()) - 1);
      }

      revalidate();
      repaint();
   }
   
   public void nameChanged(ConfigElementEvent evt)
   {
      revalidate();
      repaint();
   }

   public void propertyValueChanged(ConfigElementEvent evt)
   { 
      revalidate();
      repaint();
   }

   public void propertyValueRemoved(ConfigElementEvent evt)
   {
      System.out.println("Property value removed...");

      revalidate();
      repaint();
   }

   private PropertyDefinition   mPropDef = null;
   private ConfigElement        mConfigElement = null;
}
