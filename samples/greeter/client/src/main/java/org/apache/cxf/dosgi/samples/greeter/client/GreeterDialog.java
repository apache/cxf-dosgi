/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.dosgi.samples.greeter.client;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GreeterDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    
    JTextField name1field;
    JTextField name2field;
    JTextField ageTextField;
    JCheckBox throwExCB;
    Object selection;

    public GreeterDialog() {
        super((Frame) null, "Invoke Remote Greeter Service", true);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));     
        setContentPane(panel);

        final JRadioButton rb1 = new JRadioButton("invoke: Map<GreetingPhrase, String> greetMe(String name);");
        rb1.setSelected(true);
        rb1.setAlignmentX(Component.LEFT_ALIGNMENT);        
        panel.add(rb1);
        
        final JPanel simplePanel = createFirstOptionPanel();
        rb1.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                enablePanel(simplePanel, rb1.isSelected());
            }
        });
        panel.add(simplePanel);
        panel.add(new JLabel(" ")); // add a spacer
        
        final JRadioButton rb2 
            = new JRadioButton("invoke: GreetingPhrase [] greetMe(GreeterData data) throws GreeterException;");
        rb2.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(rb2);
        

        final JPanel complexPanel = createSecondOptionPanel();

        rb2.addChangeListener(new ChangeListener() {            
            public void stateChanged(ChangeEvent e) {
                enablePanel(complexPanel, rb2.isSelected());
            }
        });            
        
        panel.add(complexPanel);
        enablePanel(complexPanel, false);
        
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));        
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton b1 = new JButton("Invoke");
        buttons.add(b1);
        
        b1.addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                if (rb1.isSelected()) {
                    selection = name1field.getText();
                } else {
                    selection = new GreeterDataImpl(name2field.getText(),
                                                    new Integer(ageTextField.getText()), 
                                                    throwExCB.isSelected());
                }                
                
                setVisible(false);
            }
        });
        
        panel.add(buttons);
        
        ButtonGroup bg = new ButtonGroup();
        bg.add(rb1);
        bg.add(rb2);
        
        pack();
        setLocationRelativeTo(null); // centers frame on screen
    }
    
    private JPanel createFirstOptionPanel() {
        final JPanel simplePanel = new JPanel(new GridBagLayout());
        simplePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints c1 = new GridBagConstraints();
        
        
        JLabel lb1 = new JLabel("Name: ");
        c1.weightx = 0.0;
        c1.gridx = 0;
        c1.gridy = 0;
        c1.insets = new Insets(0, 25, 0, 0);
        c1.anchor = GridBagConstraints.LINE_START;
        simplePanel.add(lb1, c1);
        
        name1field = new JTextField(20);
        c1.weightx = 0.2;
        c1.gridx = 1;
        c1.gridy = 0;
        c1.insets = new Insets(0, 10, 0, 0);
        c1.anchor = GridBagConstraints.LINE_START;
        simplePanel.add(name1field, c1);
        return simplePanel;
    }
    private JPanel createSecondOptionPanel() {
        final JPanel complexPanel = new JPanel(new GridBagLayout());
        complexPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints c2 = new GridBagConstraints();
       
        JLabel lb2 = new JLabel("Name: ");
        c2.weightx = 0.0;
        c2.gridx = 0;
        c2.gridy = 0;
        c2.insets = new Insets(0, 25, 0, 0);
        c2.anchor = GridBagConstraints.LINE_START;
        complexPanel.add(lb2, c2);
        
        name2field = new JTextField(20);
        c2.weightx = 0.2;
        c2.gridx = 1;
        c2.gridy = 0;
        c2.insets = new Insets(0, 10, 0, 0);
        c2.anchor = GridBagConstraints.LINE_START;
        complexPanel.add(name2field, c2);        
                
        JLabel lb3 = new JLabel("Age: ");
        c2.weightx = 0.0;
        c2.gridx = 0;
        c2.gridy = 1;
        c2.insets = new Insets(0, 25, 0, 0);
        c2.anchor = GridBagConstraints.LINE_START;
        complexPanel.add(lb3, c2);
        
        ageTextField = new JTextField(7);
        c2.weightx = 0.2;
        c2.gridx = 1;
        c2.gridy = 1;
        c2.insets = new Insets(0, 10, 0, 0);
        c2.anchor = GridBagConstraints.LINE_START;
        complexPanel.add(ageTextField, c2);
        
        throwExCB = new JCheckBox("Throw Exception");
        c2.weightx = 0.0;
        c2.gridx = 0;
        c2.gridy = 2;
        c2.gridwidth = 2;
        c2.insets = new Insets(0, 22, 0, 0);
        c2.anchor = GridBagConstraints.LINE_START;
        complexPanel.add(throwExCB, c2);
        return complexPanel;
    }    
    
    public Object getSelection() {
        return selection;
    }
    
    private static void enablePanel(JPanel panel, boolean b) {
        for (Component c : panel.getComponents()) {
            c.setEnabled(b);
        }
    }

    public static void main(String ... args) {
        GreeterDialog gd = new GreeterDialog();
        gd.setVisible(true);
        System.exit(0);
    }
}
