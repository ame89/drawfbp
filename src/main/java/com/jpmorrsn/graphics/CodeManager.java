package com.jpmorrsn.graphics;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.net.MalformedURLException; //import java.util.regex.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;

import com.jpmorrsn.graphics.DrawFBP.FileChooserParms;
import com.jpmorrsn.graphics.DrawFBP.GenLang;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class CodeManager implements ActionListener, DocumentListener {

	DrawFBP driver;
	HashSet<String> portNames;
	HashMap<String, Integer> blocklist;
	//HashMap<String, Integer> portlist;
	Style baseStyle, normalStyle, packageNameStyle, errorStyle,
			quotedStringStyle, commentStyle;
	JFrame frame;
	StyledDocument doc;
	boolean changed = false;
	boolean generated = false;
	boolean packageNameChanged = false;
	//String targetLang;

	//final boolean SAVE_AS = true;
	boolean error = false;

	boolean fbpMode;  // generating .fbp notation
	String ext;
	Diagram diag;
	// Font font;
	String packageName = null;
	LinkedList<String> counterList = new LinkedList<String>();
	JTextPane textPane;
	JScrollPane scrollPane;

	//GenLang compLang;
	HashMap<Integer, String> descArray = new HashMap<Integer, String>();
	//HashMap<Integer, String> cdescArray = new HashMap<Integer, String>();
	//int type;
	JLabel nsLabel = null;
	boolean SAVE_AS = true;
	FileChooserParms[] saveFCPArr;
	String langLabel;
	GenLang gl = null;
	String upPort = null;;
	String dnPort = null;

	CodeManager(Diagram d) {
		diag = d;
		driver = d.driver;
		frame = new JFrame();		
		DrawFBP.applyOrientation(frame);   
		
		//type = DrawFBP.GENCODE;

		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent ev) {
				Boolean res = true;
				if (changed)
					res = askAboutSaving();
				if (res)
					frame.dispose();
			}

		});
		frame.setJMenuBar(createMenuBar());
		frame.repaint();

		
			
		BufferedImage image = driver.loadImage("DrawFBP-logo-small.png");
		frame.setIconImage(image);
		
		frame.setSize(1000, 750);
		frame.setLocation(100, 50);
		frame.setForeground(Color.WHITE);
		// jframe.setVisible(false);

		StyleContext sc = new StyleContext();
		doc = new DefaultStyledDocument(sc);
		textPane = new JTextPane(doc);
		scrollPane = new JScrollPane(textPane);
		setStyles(sc);
		frame.setVisible(true);
		textPane.setVisible(true);
		scrollPane.setVisible(true);
		frame.add(scrollPane);
		textPane.setFont(driver.fontf);
		frame.setFont(driver.fontf);
		doc.addDocumentListener(this);

	}
	void genCode() {
		if (!generateCode()) {
			frame.dispose();			
		}
	}

	boolean generateCode() {

		fbpMode = false;
		langLabel = diag.diagLang.label;
		gl = diag.diagLang;
		
		//diag.targetLang = langLabel;
		changed = true;
		diag.fCPArr[DrawFBP.COMPONENT] = driver.new FileChooserParms(diag.diagLang.srcDirProp, "Select "
				+ diag.diagLang.showLangs() + " component from directory",
				diag.diagLang.suggExtn, diag.diagLang.filter, "Components: "
						+ diag.diagLang.showLangs() + " " + diag.diagLang.showSuffixes());
		
		diag.fCPArr[DrawFBP.GENCODE] = driver.new FileChooserParms(diag.diagLang.netDirProp,
				"Specify file name for generated code",
				"." + diag.diagLang.suggExtn, diag.diagLang.filter,
				diag.diagLang.label);	
		
		String component = (langLabel.equals("Java"))
				? "component"
				: "Component";
		//String connect = (langLabel.equals("Java")) ? "connect" : "Connect";
		String initialize = (langLabel.equals("Java"))
				? "initialize"
				: "Initialize";
		String _port = (langLabel.equals("Java")) ? "port" : "Port";
		String sDO = (langLabel.equals("Java")) ? "setDropOldest()" : "SetDropOldest()";

		//if (diag.genCodeFileName == null)
		//frame.setTitle("Generated Code: " + diag.title);
			frame.setTitle("Generated Code for " + diag.diagFile.getName());
		//else {
		//	File f = new File(diag.genCodeFileName);
		//	frame.setTitle("Generated Code: " + f.getName());
		//}

		frame.setJMenuBar(createMenuBar());
		//frame.repaint();
		
		BufferedImage image = driver.loadImage("DrawFBP-logo-small.png");
		frame.setIconImage(image);

		String code = "";

		// boolean error = false;
		portNames = new HashSet<String>();
		blocklist = new HashMap<String, Integer>();

		ext = "Network";

		Style[] styles = new Style[20];

		for (Block block : diag.blocks.values()) {
			if (block instanceof ExtPortBlock) {
				ext = "SubNet";
				if (block.description == null || block.description.equals("")) {
					block.description = "IN/OUT";
					block.description = makeUniqueDesc(block.description);
				}
			}
		}

		if (langLabel.equals("Java")) {
			packageName = driver.properties.get("currentPackageName");
			if (packageName == null) {
				packageName = (String) MyOptionPane.showInputDialog(frame,
						"Please fill in a package/namespace name", null);
				packageName = packageName.trim();
				driver.properties.put("currentPackageName", packageName);
				driver.propertiesChanged = true;
			}
		}

		String[] contents;
		if (langLabel.equals("JSON")) {
			contents = new String[1];
			contents[0] = generateJSON();
			styles[0] = normalStyle;
		} else {
			contents = new String[20];
			if (langLabel.equals("Java")){
				contents[0] = "package ";
				contents[1] = packageName + ";";
			}
			else
				contents[0] = "namespace {";
			

			contents[2] = "    // change this if you want \n ";
			
			if (langLabel.equals("Java"))
				contents[2] += "import com.jpmorrsn.fbp.engine.*; \n";

			if (ext.equals("SubNet"))
				contents[3] = genMetadata(langLabel) + "\n";
			else
				contents[3] = "";

			contents[4] = "public class ";
			contents[5] = diag.title;
			if (langLabel.equals("Java"))
				contents[6] = " extends ";
			else
				contents[6] = " : ";
			contents[7] = ext;
			if (langLabel.equals("Java"))
				contents[8] = " {\nString description = ";
			else
				contents[8] = " {\nstring description = ";
			if (diag.desc == null)
				diag.desc = "";
			contents[9] = "\"" + diag.desc + "\"";
			if (langLabel.equals("Java"))
				contents[10] = ";\n protected void define() { \n";
			else
				contents[10] = ";\n public override void Define() { \n";

			styles[0] = normalStyle;
			styles[1] = packageNameStyle;
			styles[2] = baseStyle;
			styles[3] = packageNameStyle;
			styles[4] = normalStyle;
			styles[5] = packageNameStyle;
			styles[6] = normalStyle;
			styles[7] = packageNameStyle;
			styles[8] = normalStyle;
			styles[9] = packageNameStyle;
			styles[10] = normalStyle;

			for (Block block : diag.blocks.values()) {
				String s = "";
				if (block instanceof ComponentBlock
						&& block.description == null) {
					MyOptionPane.showMessageDialog(frame,
							"One or more missing block descriptions");
					error = true;
					return false;
				}

				//s = block.description;

				String t;

				if (block instanceof ComponentBlock) {

					cleanDesc(block);
					String c = cleanComp(block);
					s = block.descMod;
					
					if (!block.multiplex)
						code += genComp(s, c, langLabel) + "; \n";
					else {
						if (block.mpxfactor == null) {
							String d = (String) MyOptionPane.showInputDialog(frame,									
									"Multiplex factor for " +
											"\"" + block.description + "\"",
											"Please enter number",
									JOptionPane.QUESTION_MESSAGE);
							if (d == null || d.equals("")) {
								block.mpxfactor = "????";
								error = true;
							} else
								block.mpxfactor = d;
							diag.changed = true;
						}

						frame.repaint();
						if (block.mpxfactor != null) {
							code += "int " + compress(block.descMod)
									+ "_count = " + block.mpxfactor + "; "
									+ "     //  multiplex counter for " +
									"\"" + block.descMod + "\"\n";
						}

						code += "for (int i = 0; i < "
								+ compress(block.descMod)
								+ "_count; i++)\n";
						// code += component + "(\"" + s + ":\" + i, " + c +
						// "); ";

						code += genCompMpx(s, c, langLabel) + "; ";
						if (c.equals("????")) {
							code += "       // <=== fill in component name";
						}
						code += "\n";
					}

				}
				
				
				
				if (block instanceof ExtPortBlock) {
					ExtPortBlock eb = (ExtPortBlock) block;
					if (block.type.equals(Block.Types.EXTPORT_IN_BLOCK)) {
						s = "SUBIN";
						if (eb.substreamSensitive)
							t = "SubInSS";
						else
							t = "SubIn";
					} else if (block.type.equals(Block.Types.EXTPORT_OUT_BLOCK)) {
						s = "SUBOUT";
						if (eb.substreamSensitive)
							t = "SubOutSS";
						else
							t = "SubOut";
					} else {
						s = "SUBOI";
						t = "SubOI";
					}
					s = makeUniqueDesc(s); // and make it unique
					// if (!(t.toLowerCase().endsWith(".class")))
					// t += ".class";
					if (t.toLowerCase().endsWith(".class"))
						t = t.substring(0, t.length() - 6);

					code += genComp(s, t, langLabel) + "; \n";
					code += initialize + "(\"" + block.descMod + "\", "
							+ component + "(\"" + s + "\"), " + _port
							+ "(\"NAME\")); \n";
				}

				if (block instanceof IIPBlock) 
					descArray.put(new Integer(block.id), block.description);
				else
					descArray.put(new Integer(block.id), block.descMod);
				//cdescArray.put(new Integer(block.id), s);
			}

			for (Arrow arrow : diag.arrows.values()) {
				// generate a connection or initialize
				Block from = diag.blocks.get(new Integer(arrow.fromId));
				Arrow a2 = arrow.findTerminalArrow();
				Block to = diag.blocks.get(new Integer(a2.toId));
				if (to == null) {
					MyOptionPane.showMessageDialog(frame,
							"Downstream block not found");
					break;
				}
				if (from == null || to == null || from instanceof FileBlock
						|| from instanceof ReportBlock
						|| from instanceof LegendBlock
						|| to instanceof FileBlock || to instanceof ReportBlock
						|| to instanceof LegendBlock)
					continue;

				if (!getPortNames(arrow))
					 return false;

				frame.repaint();

				String fromDesc = descArray.get(new Integer(arrow.fromId));
				// String cFromDesc = cdescArray.get(new Integer(arrow.fromId));

				String toDesc = descArray.get(new Integer(a2.toId));
				// String cToDesc = cdescArray.get(new Integer(a2.toId));

				// boolean ok;
				
				
				// }

				frame.repaint();
				//String upPort = arrow.upStreamPort;
				//String dnPort = a2.downStreamPort;
				
				String cap = "";
				if (arrow.capacity > 0)
					cap = ", " + arrow.capacity;
				if (from instanceof ComponentBlock
						&& to instanceof ComponentBlock) {
					
					if (!arrow.endsAtLine
							&& checkDupPort(dnPort, to)) {
						MyOptionPane.showMessageDialog(frame,
								"Duplicate port name: " + dnPort);
						error = true;
					}
					if (checkDupPort(upPort, from)) {
						MyOptionPane.showMessageDialog(frame,
								"Duplicate port name: " + upPort);
						error = true;
					}
					if (from.multiplex) {
						code += "for (int i = 0; i < " + compress(fromDesc)
								+ "_count; i++)\n";
						code += genConnect(arrow) + "(" + component + "(\"" + fromDesc
								+ ":\" + i), " + _port + "(" + q(upPort) + "), "
								+ component + "(\"" + toDesc + "\"), " + _port
								+ "(" + q(dnPort) + ")" + cap + "); \n";
						if (arrow.dropOldest)
							code += "c" + arrow.id + "." + sDO + "; \n";
					} else if (to.multiplex) {
						code += "for (int i = 0; i < " + compress(toDesc)
								+ "_count; i++)\n";
						code += genConnect(arrow) + "(" + component + "(" + q(fromDesc)
								+ "), " + _port + "(" + q(upPort) + ",i), "
								+ component + "(\"" + toDesc + ":\" + i), "
								+ _port + "(" + q(dnPort) + ")" + cap + "); \n";
						if (arrow.dropOldest)
						code += "c" + arrow.id + "." + sDO + "; \n";
					} else {
						code += genConnect(arrow) + "(" + component + "(" + q(fromDesc)
								+ "), " + _port + "(" + q(upPort) + "), "
								+ component + "(\"" + toDesc + "\"), " + _port
								+ "(" + q(dnPort) + ")" + cap + "); \n";
						if (arrow.dropOldest)
					code += "c" + arrow.id + "." + sDO + "; \n";
					}
				}

				else if (from instanceof IIPBlock
						&& to instanceof ComponentBlock) {
					if (!arrow.endsAtLine
							&& checkDupPort(dnPort, to)) {
						MyOptionPane.showMessageDialog(frame,
								"Duplicate port name: " + dnPort);
						error = true;
					}

					code += initialize + "(" + q(fromDesc) + ", " + component
							+ "(\"" + toDesc + "\"), " + _port + "("
							+ q(dnPort) + ")" + cap + "); \n";
				}

				if (from instanceof ExtPortBlock) {

					code += genConnect(arrow) + "(" + component + "(" + q(fromDesc)
							+ "), " + _port + "(\"OUT\"), " + component
							+ "(\"" + toDesc + "\"), " + _port + "("
							+ q(dnPort) + ")" + cap + "); \n";
					if (arrow.dropOldest)
					code += "c" + arrow.id + "." + sDO + "; \n";
				} else if (to instanceof ExtPortBlock) {

					code += genConnect(arrow) + "(" + component + "(" + q(fromDesc)
							+ "), " + _port + "(" + q(upPort) + "), " + component
							+ "(\"" + toDesc + "\"), " + _port
							+ "(\"IN\" " + ")" + cap + "); \n";
					if (arrow.dropOldest)
					code += "c" + arrow.id + "." + sDO + "; \n";
				}
			}

			if (ext.equals("Network")) {
				String s = diag.title;
				int i = s.indexOf(".");
				if (i > -1)
					s = s.substring(0, i);
				code += "\n } \n";
				if (langLabel.equals("Java"))
					code += "public static void main(String[] argv) throws Exception  { \n"
							+ " new " + s + "().go(); \n";
				else
					code += "internal static void main(String[] argv) { \n"
							+ " new " + s + "().Go();\n }\n";
			}

			code += "} \n \n";

			int sno = 11;
			contents[sno] = code;
			styles[sno] = normalStyle;

			sno++;

			contents[sno] = "}\n";
			styles[sno] = normalStyle;
			sno++;

			if (error) {
				contents[sno] = "\n /* Errors in generated code - they must be corrected for your program to run - \n\n"
						+ "               remove this comment when you are done  */";
				styles[sno] = errorStyle;
			}
		}
		// insert data from arrays
		try {
			for (int i = 0; i < contents.length; i++) {
				if (contents[i] != null)					
				doc.insertString(doc.getLength(), contents[i], styles[i]);
			}
		} catch (BadLocationException ble) {
			MyOptionPane.showMessageDialog(frame,
					"Couldn't insert text into text pane");
			return false;
		}

		//if (diag.compLang != glcompLang)
		//	diag.compLang = compLang;
		changed = true;
		colourCode();

		generated = true;

        nsLabel.setText("Not saved");

		frame.repaint();
		// jframe.update(jdriver.osg);

		return true;
	}

	String genComp(String name, String className, String lang) {
		if (className == null)
			className = "????";
		if (lang.equals("Java"))
			return "component(\"" + name + "\"," + className + ".class)";
		else
			return "Component(\"" + name + "\", typeof(" + className + "))";
	}
	
	String genConnect(Arrow arrow) {		
		String connect = (langLabel.equals("Java")) ? "connect" : "Connect";
		if (arrow.dropOldest){			
			connect = "Connection c" + arrow.id + " = " + connect ;
		}
		return connect;	
	}

	String genCompMpx(String name, String className, String lang) {
		if (className == null)
			className = "????";
		if (lang.equals("Java"))
			return "component(\"" + name + ":\" + i," + className + ".class)";
		else
			return "Component(\"" + name + ":\" + i, typeof(" + className
					+ "))";
	}
	

	void display(File file, GenLang gl) {

		frame.setTitle("Displayed Code: " + file.getName());
		// genLang = gl;
		
		String fileString = driver.curDiag.readFile(file);
		if (fileString == null) {
			MyOptionPane.showMessageDialog(frame,
					"Couldn't read file: " + file.getAbsolutePath());
			return;
		}
		// changed = false;
		if (file.getName().endsWith(".fbp")) {
			nsLabel.setText("Not changed");
			fbpMode = true;
			changed = false;
		}
		String suff = driver.curDiag.getSuffix(file.getName());

		if (suff != null && suff.toLowerCase().equals("java")) {
			int i = fileString.indexOf("package ");
			if (i == -1) {
				packageName = (String) MyOptionPane.showInputDialog(frame,
						"Missing package name - please specify package name",
						null);

				// driver.properties.put("currentPackageName", packageName);
				// driver.propertiesChanged = true;
				fileString = "package " + packageName + ";\n" + fileString;
				changed = true;
			} else {
				int k = fileString.indexOf(";", i);
				packageName = fileString.substring(i + 8, k);
			}
			packageName = packageName.trim();
			String pkg = (String) driver.properties.get("currentPackageName");
			if (pkg != null && !(pkg.equals(packageName))) {
				driver.properties.put("currentPackageName", packageName);
				driver.propertiesChanged = true;
			}
		}

		try {
			doc.insertString(0, fileString, normalStyle);
		} catch (BadLocationException ble) {
			MyOptionPane.showMessageDialog(frame,
					"Couldn't insert text into text pane");
			return;
		}

		colourCode();
		//if (file.getName().endsWith(".fbp"))
		//	type = DrawFBP.DIAGRAM;  
		frame.repaint();
		//frame.repaint();
		return;
	}

	String genMetadata(String lang) {
		String inData = "";
		String outData = "";
		String descr = "";
		if (diag.desc != null) {
			if (lang.equals("Java"))
				descr = "@ComponentDescription(\"" + diag.desc + "\") \n";
			else
				descr = "[ComponentDescription(\"" + diag.desc + "\")] \n";
		}

		int ins = 0;
		int outs = 0;
		for (Block block : diag.blocks.values()) {
			if (block instanceof ExtPortBlock) {
				if (block.type.equals(Block.Types.EXTPORT_IN_BLOCK)) {
					if (lang.equals("Java")) {
						ins++;
						inData += ", @InPort(\"" + block.description + "\")";
					} else {
						inData += "[InPort(\"" + block.description + "\")] \n";
					}
				} else if (block.type.equals(Block.Types.EXTPORT_OUT_BLOCK)) {
					if (lang.equals("Java")) {
						outs++;
						outData += ", @OutPort(\"" + block.description + "\")";
					} else {
						outData += "[OutPort(\"" + block.description
								+ "\")] \n";
					}
				}
			}
		}
		if (lang.equals("Java")) {
			inData = inData.substring(2);
			if (ins > 1)
				inData = "@InPorts({" + inData + "})";
			inData += "\n";
			outData = outData.substring(2);
			if (outs > 1)
				outData = "@OutPorts({" + outData + "})";
			outData += "\n";
		}
		return descr + inData + outData;
	}

	

	String makeUniqueDesc(String s) {
		Integer i;
		String t = s;
		if (blocklist.containsKey(s)) {
			i = blocklist.get(s);
			i = new Integer(i.intValue() + 1);
			t = s + "_" + i.toString() + "_";
		} else
			i = new Integer(1);
		blocklist.put(s, i);
		return t;

	}
	
	String makeUniquePort(String s, Block b) {
		Integer i;
		String t = s;
		if (b.portlist == null)
			b.portlist = new HashMap<String, Integer>();
		if (b.portlist.containsKey(s)) {
			i = b.portlist.get(s);
			i = new Integer(i.intValue() + 1);
			t = s + "_" + i.toString() + "_";
		} else
			i = new Integer(1);
		b.portlist.put(s, i);
		return t;

	}
	


	boolean checkDupPort(String port, Block bl) {
		// return false if no duplication
		String s = port + ":" + bl.id;
		if (portNames.contains(s))
			return true;
		portNames.add(s);
		return false;
	}

	void setStyles(StyleContext sc) {

		Font font = driver.fontf;
		// baseStyle = new SimpleAttributeSet();
		Style defaultStyle = sc.getStyle(StyleContext.DEFAULT_STYLE);
		baseStyle = sc.addStyle(null, defaultStyle);
		int s = font.getSize();

		StyleConstants.setSpaceAbove(baseStyle, 4);
		StyleConstants.setSpaceBelow(baseStyle, 4);
		StyleConstants.setForeground(baseStyle, Color.DARK_GRAY);
		StyleConstants.setFontFamily(baseStyle, font.getFamily());
		StyleConstants.setFontSize(baseStyle, s);

		normalStyle = sc.addStyle(null, baseStyle);
		StyleConstants.setForeground(normalStyle, Color.BLUE);
		// StyleConstants.setFontSize(normalStyle, s);

		packageNameStyle = sc.addStyle(null, baseStyle);
		StyleConstants.setForeground(packageNameStyle, Color.MAGENTA);
		// StyleConstants.setFontSize(packageNameStyle, s);

		errorStyle = sc.addStyle(null, baseStyle);
		StyleConstants.setForeground(errorStyle, Color.RED);
		// StyleConstants.setFontSize(errorStyle, s);

		quotedStringStyle = sc.addStyle(null, baseStyle);
		StyleConstants.setForeground(quotedStringStyle, new Color(178, 34, 34));// Firebrick
		// StyleConstants.setFontSize(quotedStringStyle, s);

		commentStyle = sc.addStyle(null, baseStyle);
		StyleConstants.setForeground(commentStyle, new Color(46, 139, 87));// Sea
																			// green
		// StyleConstants.setFontSize(commentStyle, s);

	}

	void colourCode() {
		boolean packageRead = false;
		for (int i = 0; i < doc.getLength(); i++)
			try {
				if (!packageRead && doc.getText(i, 8).equals("package ")) {
					packageRead = true;
					i += 8;
					int j = doc.getText(i, doc.getLength() - 9).indexOf(";");
					doc.setCharacterAttributes(i, j, packageNameStyle, false); // Magenta
				}

				if (doc.getText(i, 4).equals("????"))
					doc.setCharacterAttributes(i, 4, errorStyle, false); // Red

				if (doc.getText(i, 10).equals("null.class"))
					doc.setCharacterAttributes(i, 10, errorStyle, false); // Red

				if (doc.getText(i, 1).equals("@")) {
					int j;
					for (j = i; j < doc.getLength(); j++) {
						if (doc.getText(j, 1).equals("\n"))
							break;
					}
					doc.setCharacterAttributes(i, j - i, packageNameStyle,
							false); // Magenta
				}

				if (doc.getText(i, 1).equals("\"")
						&& !(doc.getText(i - 1, 1).equals(File.separator))) {
					int j = i + 1;
					for (; j < doc.getLength(); j++) {
						if (doc.getText(j, 1).equals("\"")
								&& !(doc.getText(j - 1, 1)
										.equals(File.separator)))
							break;
					}
					doc.setCharacterAttributes(i, j - i + 1, quotedStringStyle,
							false); // firebrick
					i = j; // skip quoted string
				}

				if (doc.getText(i, 2).equals("/*")) {
					int j = i + 2;
					for (; j < doc.getLength(); j++) {
						if (doc.getText(j, 2).equals("*/"))
							break;
					}
					doc.setCharacterAttributes(i, j - i + 2, commentStyle,
							false); // green
					i = j + 2; // skip comment
				}
				if (doc.getText(i, 2).equals("//")) {
					int j = i + 2;
					String ls = "\n";
					for (; j < doc.getLength(); j++) {
						if (doc.getText(j, ls.length()).equals(ls))
							break;
						//String x = doc.getText(j, ls.length());
						//x += "";
					}
					doc.setCharacterAttributes(i, j - i + 1, commentStyle,
							false); // green
					i = j + ls.length(); // skip line separator
				}

				if (doc.getText(i, 3).equals("<==")) {
					int j = i + 3;
					for (; j < doc.getLength(); j++) {
						if (doc.getText(j, 1).equals("\n"))
							break;
					}
					doc.setCharacterAttributes(i, j - i, baseStyle, false); // Gray
				}

			} catch (BadLocationException e) {
			}
		textPane.setFont(driver.fontf);
		scrollPane.setFont(driver.fontf);
		frame.setFont(driver.fontf);

	}

	public void actionPerformed(ActionEvent e) {
		String s = e.getActionCommand();

		//if (s.equals("Save")) {
		//	saveCode(/*!SAVE_AS*/);
		//} else 
			if (s.equals("Save As")) {
				
			saveCode(/*SAVE_AS*/);

		} else if (s.equals("Exit")) {
			Boolean res = true;
			if (changed)
				res = askAboutSaving();
			if (res)
				frame.dispose();

		}
		return;
	}

	public boolean askAboutSaving() {
		int answer = MyOptionPane.showConfirmDialog(frame,  
				"Save generated or modified code?", "Save code",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.INFORMATION_MESSAGE);
		
		boolean b; 
		if (answer == JOptionPane.YES_OPTION) {
			// User clicked YES.
			b = saveCode(/*SAVE_AS*/);
			//diag.diagLang = gl;
			return b;
		}
		
		b = (answer == JOptionPane.NO_OPTION);
		//diag.diagLang = gl;
		return b;
	}

	public void changedUpdate(DocumentEvent e) {
		if (e.getOffset() > 0)
			changed = true;

		if (packageName != null) {
			if (e.getOffset() >= 8 && e.getOffset() <= 8 + packageName.length()
					&& generated) {
				packageNameChanged = true;				
			}
		}		
		nsLabel.setText(changed ? "Not saved" : " ");
		frame.repaint();
	}

	public void insertUpdate(DocumentEvent e) {
		if (e.getOffset() > 0)
				changed = true;

		if (packageName != null) {
			if (e.getOffset() >= 8 && e.getOffset() <= 8 + packageName.length()
					&& generated) {
				packageNameChanged = true;	
			}
		}		
		nsLabel.setText(changed ? "Not saved" : " ");
		frame.repaint();
	}

	public void removeUpdate(DocumentEvent e) {
		if (e.getOffset() > 0)
			changed = true;

		if (packageName != null) {
			if (e.getOffset() >= 8 && e.getOffset() <= 8 + packageName.length()
					&& generated) {
				packageNameChanged = true;
			}
		}		
		nsLabel.setText(changed ? "Not saved" : " ");
		frame.repaint();
	}

	String compress(String s) {
		if (counterList.indexOf(s) == -1)
			counterList.add(s);
		return "X$" + counterList.indexOf(s);
	}

	boolean saveCode ( /*boolean saveAs */)   {

		String fileString = null;
		try {
			fileString = doc.getText(0, doc.getLength());
		} catch (BadLocationException ble) {
			MyOptionPane.showMessageDialog(frame,
					"Couldn't get text from text pane");
			// diag.changeCompLang();
			return false;
		}

		File file = null;
		//if (diag.genCodeFileName != null)
		//	file = new File(diag.genCodeFileName);

		//if (file == null)
		//	saveAs = true;
		//if (saveAs)
		 	file = null;

		if (diag.fCPArr[DrawFBP.GENCODE].fileExt.equals(".java")) {
			try {
				String t = doc.getText(0, doc.getLength());
				int i = t.indexOf("package");
				if (i > -1) {
					int j = t.indexOf(";", i);
					if (j > -1) {
						String s = doc.getText(i + 8, j - i - 8);
						s = s.trim();
						if (packageName != null && !(packageName.equals(s))) {
							packageName = s;
							driver.properties.put("currentPackageName",
									packageName);
							driver.propertiesChanged = true;
							MyOptionPane.showMessageDialog(frame,
									"Package name changed: " + packageName);
						}
					}
				}

			} catch (BadLocationException ble) {
				ble.printStackTrace();
			}
		}

		file = diag.genSave(file, diag.fCPArr[DrawFBP.GENCODE], fileString, frame);

		if (file == null) {
			// MyOptionPane.showMessageDialog(frame, "File not saved");
			// diag.changeCompLang();
			return false;
		}

		//genCodeFileName = file.getAbsolutePath();
		driver.properties.put(diag.diagLang.netDirProp, file.getParent());
		driver.propertiesChanged = true;
		changed = false;
			
		if (packageNameChanged) {
			driver.properties.put("packageName", packageName);
			driver.propertiesChanged = true;
		}

				
		//diag.targetLang = langLabel;
		nsLabel.setText(changed ? "Not saved" : " ");
		//diag.genCodeFileName = file.getAbsolutePath();
		frame.setTitle("Generated Code: " + file.getName());
		frame.repaint();

		return true;
	}

	

	public JMenuBar createMenuBar() {

		JMenuBar menuBar;

		// Create the menu bar.
		menuBar = new JMenuBar();
		
		menuBar.setBorderPainted(true);

		//Box box = new Box(BoxLayout.X_AXIS);
		//box.setLayout(new FlowLayout(FlowLayout.LEFT));
		JMenuItem menuItem =
		/* new JMenuItem("Save");
		menuBar.add(menuItem);
		menuItem.addActionListener(this);
		menuItem.setBorderPainted(true);
		menuItem = 
		*/
				new JMenuItem("Save As");
		menuBar.add(menuItem);
		menuItem.addActionListener(this);
		menuItem.setBorderPainted(true);
		menuItem = new JMenuItem("Exit");
		menuBar.add(menuItem);
		menuItem.addActionListener(this);
		menuItem.setBorderPainted(true);
		//menuBar.add(box);
		//int w = frame.getWidth();
		menuBar.add(Box.createHorizontalStrut(200));
		JPanel p = new JPanel();
		nsLabel = new JLabel("Not changed");		
		p.add(nsLabel, BorderLayout.LINE_END);
		menuBar.add(p, BorderLayout.LINE_END);
		nsLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
		nsLabel.setBackground(Color.WHITE);

		return menuBar;
	}

	String generateJSON() {
		String data;
		data = "{\n\"properties\": {\n\"name\": ";
		data += q(diag.title) + "\n},\n";
		data += "\"processes\": {\n";
		portNames = new HashSet<String>();
		blocklist = new HashMap<String, Integer>();
		
		String comma = "";
		for (Block block : diag.blocks.values()) {
			// String s = "";
			if (block instanceof ComponentBlock && block.description == null) {
				MyOptionPane.showMessageDialog(frame,
						"One or more missing block descriptions");
				// error = true;
				return null;
			}
			
			if (block instanceof ComponentBlock) {
				cleanDesc(block);
				String s = cleanComp(block);
				data += comma + q(block.descMod) + ":{ \"component\" :" + q(s)
						+ ", \"display\": { \"x\":" + block.cx + ", \"y\":"
						+ block.cy + "}}";
				comma = "\n,";
			 
			    descArray.put(new Integer(block.id), block.descMod);
			}
			//cdescArray.put(new Integer(block.id), block.description);
			if (block instanceof IIPBlock) {
				descArray.put(new Integer(block.id), block.description);
			}
		}
		data += "\n},\n \"connections\": [\n";
		comma = "";
		for (Arrow arrow : diag.arrows.values()) {
			// generate a connection or initialize
			Block from = diag.blocks.get(new Integer(arrow.fromId));
			Arrow a2 = arrow.findTerminalArrow();
			Block to = diag.blocks.get(new Integer(a2.toId));
			if (to == null) {
				MyOptionPane.showMessageDialog(frame,
						"Downstream block not found");
				break;
			}
			if (from == null || to == null || from instanceof FileBlock
					|| from instanceof ReportBlock
					|| from instanceof LegendBlock || to instanceof FileBlock
					|| to instanceof ReportBlock || to instanceof LegendBlock)
				continue;

			 if (!getPortNames(arrow))
				 return "";
			String fromDesc = descArray.get(new Integer(arrow.fromId));
			//String cFromDesc = cdescArray.get(new Integer(arrow.fromId));

			String toDesc = descArray.get(new Integer(a2.toId));
			
			frame.repaint();
			if (!(from instanceof ComponentBlock)
					&& !(from instanceof IIPBlock)
					|| !(to instanceof ComponentBlock))
				continue;

			data += comma + "{ ";
			//String upPort = arrow.upStreamPort;
			//String dnPort = a2.downStreamPort;
			upPort = upPort.toLowerCase();
			upPort = makeUniquePort(upPort, from);
			dnPort = dnPort.toLowerCase();
			if (a2.dspMod == null)
				a2.dspMod = makeUniquePort(dnPort, to);
			//upPort = arrow.uspMod;
			dnPort = a2.dspMod;
			if (from instanceof IIPBlock) {
				if (!arrow.endsAtLine && checkDupPort(dnPort, to)) {
					MyOptionPane.showMessageDialog(frame,
							"Duplicate port name: " + dnPort);
					error = true;
				}
				data += "\"data\":" + q(fromDesc) + ",\n";
			} else
				data += "\"src\": {\n \"process\" :" + q(fromDesc)
						+ ",\n\"port\":" + q(upPort) + "\n},";

			data += "\"tgt\": {\n \"process\" :" + q(toDesc) + ",\n\"port\":"
					+ q(dnPort) + "}\n}";
			comma = "\n,";
		}
		data += "\n]\n}";

		return data;
	}
	

	boolean genFbpCode(){
		String code = "";
		String cma = "";
		//generated = false;
		portNames = new HashSet<String>();
		blocklist = new HashMap<String, Integer>();
		//portlist = new HashMap<String, Integer>();
		//diag.targetLang = "FBP";
		saveFCPArr = diag.fCPArr;
		//gl = diag.diagLang;
		gl = driver.findGLFromLabel("FBP");
		fbpMode = true;
				diag.fCPArr[DrawFBP.GENCODE] = driver.new FileChooserParms("currentFBPNetworkDir",
				"Specify file name for generated code",
				".fbp", diag.diagLang.filter,
				"fbp");	
		
		for (Block block : diag.blocks.values()) {

			if (block instanceof ComponentBlock) {
				if (block.description == null) {
					MyOptionPane.showMessageDialog(frame,
							"One or more missing block descriptions");
					error = true;
					return false;
				}

				cleanDesc(block);
				String s = cleanComp(block);
				code += cma + block.descMod + "(" + s + ")";
				cma = ",\n";
				descArray.put(new Integer(block.id), block.descMod);
				// cdescArray.put(new Integer(block.id), s);
			}
			if (block instanceof IIPBlock) {
				descArray.put(new Integer(block.id), block.description);
			}
		}
		
		for (Arrow arrow : diag.arrows.values()) {
			Block from = diag.blocks.get(new Integer(arrow.fromId));
			Arrow a2 = arrow.findTerminalArrow();
			Block to = diag.blocks.get(new Integer(a2.toId));
			if (to == null) {
				MyOptionPane.showMessageDialog(frame,
						"Downstream block not found");
				break;
			}
			if (from == null || to == null || from instanceof FileBlock
					|| from instanceof ReportBlock
					|| from instanceof LegendBlock
					|| to instanceof FileBlock || to instanceof ReportBlock
					|| to instanceof LegendBlock)
				continue;

			if (!getPortNames(arrow))
				 return false;
			
			String fromDesc = descArray.get(new Integer(arrow.fromId));
			//String cFromDesc = cdescArray.get(new Integer(arrow.fromId));

			String toDesc = descArray.get(new Integer(a2.toId));
			//String cToDesc = cdescArray.get(new Integer(a2.toId));

			frame.repaint();
			
			if (!(from instanceof IIPBlock)) {
			upPort = arrow.upStreamPort;			
			upPort = upPort.replaceAll("-", "\\\\-");
			upPort = upPort.replaceAll("\\.", "\\\\.");
			upPort = makeUniquePort(upPort, from);
			}
			
			dnPort = a2.downStreamPort;
			//dnPort = dnPort.toLowerCase();
			
			if (a2.dspMod == null)
				a2.dspMod = makeUniquePort(dnPort, to);
			
			dnPort = a2.dspMod.replaceAll("-", "\\\\-");
			dnPort = dnPort.replaceAll("\\.", "\\\\.");
			
			if (from instanceof ComponentBlock
					&& to instanceof ComponentBlock) {
				if (!arrow.endsAtLine
						&& checkDupPort(dnPort, to)) {
					MyOptionPane.showMessageDialog(frame,
							"Duplicate port name: " + dnPort);
					error = true;
				}
				if (checkDupPort(upPort, from)) {
					MyOptionPane.showMessageDialog(frame,
							"Duplicate port name: " + upPort);
					error = true;
				}
				 
				if (from.multiplex) {
					
					MyOptionPane.showMessageDialog(frame,
							"Multiplexing not supported");
					error = true;
				} else if (to.multiplex) {
					
					MyOptionPane.showMessageDialog(frame,
							"Multiplexing not supported");
					error = true;
				} else				
					 
				code += cma + fromDesc + " " + upPort + " -> " + dnPort + " " + toDesc;
			}
			else if (from instanceof IIPBlock
					&& to instanceof ComponentBlock) {
				if (!arrow.endsAtLine
						&& checkDupPort(dnPort, to)) {
					MyOptionPane.showMessageDialog(frame,
							"Duplicate port name: " + dnPort);
					error = true;
				}			
				code += cma + "'" + fromDesc + "' -> " + dnPort + " " + toDesc;
			}

			if (from instanceof ExtPortBlock) {				
				code += cma + fromDesc + " out -> " + dnPort + " " + toDesc;
				
			} else if (to instanceof ExtPortBlock) {			
				code += cma + fromDesc + " -> in " + toDesc;
			}
		}
		 
		// insert string data
				try {					
						doc.insertString(doc.getLength(), code, baseStyle);
					
				} catch (BadLocationException ble) {
					MyOptionPane.showMessageDialog(frame,
							"Couldn't insert text into text pane");
					// restore old language parameters
					diag.fCPArr[DrawFBP.GENCODE] = driver.new FileChooserParms(diag.diagLang.netDirProp,
							"Specify file name for generated code",
							"." + diag.diagLang.suggExtn, diag.diagLang.filter,
							diag.diagLang.label);
					return false;
				}

				changed = true;
				//colourCode();

				generated = true;

		        nsLabel.setText(changed ? "Not saved" : " ");

				frame.repaint();
				// jframe.update(jdriver.osg);
				
				// restore old language parameters
				diag.fCPArr[DrawFBP.GENCODE] = driver.new FileChooserParms(diag.diagLang.netDirProp,
						"Specify file name for generated code",
						"." + diag.diagLang.suggExtn, diag.diagLang.filter,
						diag.diagLang.label);
				
				return true;
		
	}
	
	
	String q(String s) {
		return "\"" + s + "\"";
	}
	
	String cleanComp(Block b) {
		
		//String[] sa = new String[2]; // process name and component name, resp.
		
		
		//if (gl.label.equals("FBP")) 
		 //   s = cleanDesc(s); // clean up name	
				
		
		error = false;
		String c = b.fullClassName;
		if (c == null) {
			c = b.codeFileName;
			if (c == null)
				error = true;
		}
		if (!error) {
			if (gl.label.equals("JSON")) { // bit of a hack...
				int i = c.lastIndexOf(File.separator);
				if (i == -1)
					i = c.lastIndexOf("/");
				c = c.substring(i + 1);
				int j = c.lastIndexOf(".");
				if (j > -1)
					c = c.substring(0, j);
			}
			int i = c.indexOf("!");
			if (i > -1 && i < c.length() - 1)
				c = c.substring(i + 1);
			if (c.toLowerCase().endsWith(".class"))
				c = c.substring(0, c.length() - 6);
		}
		
		return c;
	}
	
	void cleanDesc(Block b) {

		String t = b.description;
		//if (!(b instanceof IIPBlock)) {

			if (t == null || t.equals(""))
				t = "_comp_";
			t = t.replace('"', '\u0007');
			Pattern p;
			Matcher ma;

			if (fbpMode) {
				t = t.replace('_', '\u0007');
				p = Pattern.compile("\\p{Punct}"); // punctuation chars
				ma = p.matcher(t);

				String u = "";
				int i = 0;
				while (ma.find(i)) {
					String s = "\\" + ma.group();
					u += t.substring(i, ma.start()) + s;
					i = ma.end();
				}
				u += t.substring(i);
				t = u;
			}

			p = Pattern.compile("\\s"); // whitespace chars
			ma = p.matcher(t);
			t = ma.replaceAll("_");

			t = t.replace('\u0007', '_');

			b.descMod = makeUniqueDesc(t); // and make it unique
		
		return;
	}
	
	boolean getPortNames(Arrow arrow) {		
		Block from = diag.blocks.get(new Integer(arrow.fromId));
		Arrow a2 = arrow.findTerminalArrow();
		Block to = diag.blocks.get(new Integer(a2.toId));
		String z;
		
		if (from instanceof ComponentBlock) {
		upPort = arrow.upStreamPort;
		while (true) {
			if (upPort != null && !(upPort.equals(""))) {
				z = validatePortName(upPort);
				if (z != null) {
					upPort = z;
					break;
				}
				if (JOptionPane.NO_OPTION == MyOptionPane
						.showConfirmDialog(null, "Invalid port name: "
								+ upPort,
								"Invalid output port name - try again?",
								JOptionPane.YES_NO_OPTION)) {
					// ok = false;
					upPort = "????";
					break;
				}
			}
			upPort = (String) MyOptionPane.showInputDialog(frame,
					"Output port from " + "\"" + from.description
							+ "\"", "Please enter port name",
					JOptionPane.QUESTION_MESSAGE);
			if (upPort == null)
					return false;
			diag.changed = true;

		}

		arrow.upStreamPort = upPort;
		
		
		 }

		dnPort = a2.downStreamPort;
		while (true) {
			if (dnPort != null && !(dnPort.equals(""))) {
				z = validatePortName(dnPort);
				if (z != null) {
					dnPort = z;
					break;
				}
				if (JOptionPane.NO_OPTION == MyOptionPane
						.showConfirmDialog(null, "Invalid port name: "
								+ dnPort,
								"Invalid input port name - try again?",
								JOptionPane.YES_NO_OPTION)) {
					// ok = false;
					dnPort = "????";
					break;
				}
			}
			dnPort = (String) MyOptionPane.showInputDialog(frame,
					"Input port to " + "\"" + to.description + "\"",
					"Please enter port name",
					JOptionPane.QUESTION_MESSAGE);
			if (dnPort == null)
				return false;
			diag.changed = true;

		}

		a2.downStreamPort = dnPort;
		
		return true;
	}
	
	String validatePortName(String s) {
		if (s == null || s.equals("") || s.equals("????")) 
			return null;
		Pattern p = Pattern.compile("[a-zA-Z][\\d\\-\\_\\.\\[\\]a-zA-Z]*");  // Allow hyphen (for Humberto), period (for Tom), underscore
		                                                               //    and square brackets                     
		Matcher ma = p.matcher(s);
		if (!ma.matches())
			return null;		
		else
			return s;
	}
}
