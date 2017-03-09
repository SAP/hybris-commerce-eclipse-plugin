package com.hybris.hyeclipse.tsv.validator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: gary
 * Date: 07/02/2013
 * Time: 11:40
 * To change this template use File | Settings | File Templates.
 */
public class LocationRecordingProgressReportingHandler extends DefaultHandler {
    public static final String KEY_LIN_NO = "com.hybris.ps.tsv.LineNumber";
    public static final String KEY_COL_NO = "com.hybris.ps.tsv.ColumnNumber";

    private final Document doc;
    private Locator locator = null;
    private Element current;
    private final SubMonitor progress;

    // The docs say that parsers are "highly encouraged" to set this
    public LocationRecordingProgressReportingHandler(Document doc, final IProgressMonitor monitor) {
        this.doc = doc;
        this.progress = SubMonitor.convert(monitor, 100);
    }

    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    protected Locator getDocumentLocator() {
        return this.locator;
    }
    
    // This just takes the location info from the locator and puts
    // it into the provided node
    protected void setLocationData(Node n) {
        if (locator != null) {
            n.setUserData(KEY_LIN_NO, locator.getLineNumber(), null);
            n.setUserData(KEY_COL_NO, locator.getColumnNumber(), null);
        }
    }

    // Admittedly, this is largely lifted from other examples
    public void startElement(
            String uri, String localName, String qName, Attributes attrs) {
    	progress.setWorkRemaining(100);
        Element e = null;
        if (localName != null && !"".equals(localName)) {
            e = doc.createElementNS(uri, localName);
        } else {
            e = doc.createElement(qName);
        }

        // But this part isn't lifted ;)
        setLocationData(e);

        if (current == null) {
            doc.appendChild(e);
        } else {
            current.appendChild(e);
        }
        current = e;

        // For each attribute, make a new attribute in the DOM, append it
        // to the current element, and set the column and line numbers.
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                Attr attr = null;
                if (attrs.getLocalName(i) != null && !"".equals(attrs.getLocalName(i))) {
                    attr = doc.createAttributeNS(attrs.getURI(i), attrs.getLocalName(i));
                    attr.setValue(attrs.getValue(i));
                    setLocationData(attr);
                    current.setAttributeNodeNS(attr);
                } else {
                    attr = doc.createAttribute(attrs.getQName(i));
                    attr.setValue(attrs.getValue(i));
                    setLocationData(attr);
                    current.setAttributeNode(attr);
                }
            }
        }
    }

    public void endElement(String uri, String localName, String qName) {
        Node parent;

        if (current == null) {
            return;
        }

        parent = current.getParentNode();
        // If the parent is the document itself, then we're done.
        if (parent.getParentNode() == null) {
            current.normalize();
            current = null;
        } else {
            current = (Element)current.getParentNode();
        }
        progress.worked(10);
    }

    // Even with text nodes, we can record the line and column number
    public void characters(char buf[], int offset, int length) {
        if (current != null) {
            Node n = doc.createTextNode(new String(buf, offset, length));
            setLocationData(n);
            current.appendChild(n);
        }
    }
}
