/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.pdmodel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;

import org.apache.pdfbox.pdmodel.common.COSArrayList;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDDestinationOrAction;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDURIDictionary;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionFactory;
import org.apache.pdfbox.pdmodel.interactive.action.PDDocumentCatalogAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

import org.apache.pdfbox.pdmodel.interactive.pagenavigation.PDThread;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;

/**
 * This class represents the acroform of a PDF document.
 *
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * @version $Revision: 1.21 $
 */
public class PDDocumentCatalog implements COSObjectable
{
    private COSDictionary root;
    private PDDocument document;

    private PDAcroForm acroForm = null;

    /**
     * Page mode where neither the outline nor the thumbnails
     * are displayed.
     */
    public static final String PAGE_MODE_USE_NONE = "UseNone";
    /**
     * Show bookmarks when pdf is opened.
     */
    public static final String PAGE_MODE_USE_OUTLINES = "UseOutlines";
    /**
     * Show thumbnails when pdf is opened.
     */
    public static final String PAGE_MODE_USE_THUMBS = "UseThumbs";
    /**
     * Full screen mode with no menu bar, window controls.
     */
    public static final String PAGE_MODE_FULL_SCREEN = "FullScreen";
    /**
     * Optional content group panel is visible when opened.
     */
    public static final String PAGE_MODE_USE_OPTIONAL_CONTENT = "UseOC";
    /**
     * Attachments panel is visible.
     */
    public static final String PAGE_MODE_USE_ATTACHMENTS = "UseAttachments";

    /**
     * Display one page at a time.
     */
    public static final String PAGE_LAYOUT_SINGLE_PAGE = "SinglePage";
    /**
     * Display the pages in one column.
     */
    public static final String PAGE_LAYOUT_ONE_COLUMN = "OneColumn";
    /**
     * Display the pages in two columns, with odd numbered pagse on the left.
     */
    public static final String PAGE_LAYOUT_TWO_COLUMN_LEFT = "TwoColumnLeft";
    /**
     * Display the pages in two columns, with odd numbered pagse on the right.
     */
    public static final String PAGE_LAYOUT_TWO_COLUMN_RIGHT ="TwoColumnRight";
    /**
     * Display the pages two at a time, with odd-numbered pages on the left.
     * @since PDF Version 1.5
     */
    public static final String PAGE_LAYOUT_TWO_PAGE_LEFT = "TwoPageLeft";
    /**
     * Display the pages two at a time, with odd-numbered pages on the right.
     * @since PDF Version 1.5
     */
    public static final String PAGE_LAYOUT_TWO_PAGE_RIGHT = "TwoPageRight";



    /**
     * Constructor.
     *
     * @param doc The document that this catalog is part of.
     */
    public PDDocumentCatalog( PDDocument doc )
    {
        document = doc;
        root = new COSDictionary();
        root.setItem( COSName.TYPE, new COSString( "Catalog" ) );
        document.getDocument().getTrailer().setItem( COSName.ROOT, root );
    }

    /**
     * Constructor.
     *
     * @param doc The document that this catalog is part of.
     * @param rootDictionary The root dictionary that this object wraps.
     */
    public PDDocumentCatalog( PDDocument doc, COSDictionary rootDictionary )
    {
        document = doc;
        root = rootDictionary;
    }

    /**
     * Convert this standard java object to a COS object.
     *
     * @return The cos object that matches this Java object.
     */
    public COSBase getCOSObject()
    {
        return root;
    }

    /**
     * Convert this standard java object to a COS object.
     *
     * @return The cos object that matches this Java object.
     */
    public COSDictionary getCOSDictionary()
    {
        return root;
    }

    /**
     * This will get the documents acroform.  This will return null if
     * no acroform is part of the document.
     *
     * @return The documents acroform.
     */
    public PDAcroForm getAcroForm()
    {
        if( acroForm == null )
        {
            COSDictionary acroFormDic =
                (COSDictionary)root.getDictionaryObject( COSName.ACRO_FORM );
            if( acroFormDic != null )
            {
                acroForm = new PDAcroForm( document, acroFormDic );
            }
        }
        return acroForm;
    }

    /**
     * Set the acro form for this catalog.
     *
     * @param acro The new acro form.
     */
    public void setAcroForm( PDAcroForm acro )
    {
        root.setItem( COSName.ACRO_FORM, acro );
    }

    /**
     * This will get the root node for the pages.
     *
     * @return The parent page node.
     */
    public PDPageNode getPages()
    {
        return new PDPageNode( (COSDictionary)root.getDictionaryObject( COSName.PAGES ) );
    }

    /**
     * The PDF document contains a hierarchical structure of PDPageNode and PDPages, which
     * is mostly just a way to store this information.  This method will return a flat list
     * of all PDPage objects in this document.
     *
     * @return A list of PDPage objects.
     */
    public List getAllPages()
    {
        List retval = new ArrayList();
        PDPageNode rootNode = getPages();
        //old (slower):
        //getPageObjects( rootNode, retval );
        rootNode.getAllKids(retval);
        return retval;
    }

    /**
     * Get the viewer preferences associated with this document or null if they
     * do not exist.
     *
     * @return The document's viewer preferences.
     */
    public PDViewerPreferences getViewerPreferences()
    {
        PDViewerPreferences retval = null;
        COSDictionary dict = (COSDictionary)root.getDictionaryObject( "ViewerPreferences" );
        if( dict != null )
        {
            retval = new PDViewerPreferences( dict );
        }

        return retval;
    }

    /**
     * Set the viewer preferences.
     *
     * @param prefs The new viewer preferences.
     */
    public void setViewerPreferences( PDViewerPreferences prefs )
    {
        root.setItem( "ViewerPreferences", prefs );
    }

    /**
     * Get the outline associated with this document or null if it
     * does not exist.
     *
     * @return The document's outline.
     */
    public PDDocumentOutline getDocumentOutline()
    {
        PDDocumentOutline retval = null;
        COSDictionary dict = (COSDictionary)root.getDictionaryObject( "Outlines" );
        if( dict != null )
        {
            retval = new PDDocumentOutline( dict );
        }

        return retval;
    }

    /**
     * Set the document outlines.
     *
     * @param outlines The new document outlines.
     */
    public void setDocumentOutline( PDDocumentOutline outlines )
    {
        root.setItem( "Outlines", outlines );
    }

    /**
     * Get the list threads for this pdf document.
     *
     * @return A list of PDThread objects.
     */
    public List getThreads()
    {
        COSArray array = (COSArray)root.getDictionaryObject( "Threads" );
        if( array == null )
        {
            array = new COSArray();
            root.setItem( "Threads", array );
        }
        List pdObjects = new ArrayList();
        for( int i=0; i<array.size(); i++ )
        {
            pdObjects.add( new PDThread( (COSDictionary)array.getObject( i ) ) );
        }
        return new COSArrayList( pdObjects, array );
    }

    /**
     * Set the list of threads for this pdf document.
     *
     * @param threads The list of threads, or null to clear it.
     */
    public void setThreads( List threads )
    {
        root.setItem( "Threads", COSArrayList.converterToCOSArray( threads ) );
    }

    /**
     * Get the metadata that is part of the document catalog.  This will
     * return null if there is no meta data for this object.
     *
     * @return The metadata for this object.
     */
    public PDMetadata getMetadata()
    {
        PDMetadata retval = null;
        COSStream stream = (COSStream)root.getDictionaryObject( "Metadata" );
        if( stream != null )
        {
            retval = new PDMetadata( stream );
        }
        return retval;
    }

    /**
     * Set the metadata for this object.  This can be null.
     *
     * @param meta The meta data for this object.
     */
    public void setMetadata( PDMetadata meta )
    {
        root.setItem( "Metadata", meta );
    }

    /**
     * Set the Document Open Action for this object.
     *
     * @param action The action you want to perform.
     */
    public void setOpenAction( PDDestinationOrAction action )
    {
        root.setItem( COSName.getPDFName("OpenAction"), action );
    }

    /**
     * Get the Document Open Action for this object.
     *
     * @return The action to perform when the document is opened.
     *
     * @throws IOException If there is an error creating the destination
     * or action.
     */
    public PDDestinationOrAction getOpenAction() throws IOException
    {
        PDDestinationOrAction action = null;
        COSBase actionObj = root.getDictionaryObject("OpenAction");

        if( actionObj == null )
        {
            //no op
        }
        else if( actionObj instanceof COSDictionary )
        {
            action = PDActionFactory.createAction((COSDictionary)actionObj);
        }
        else if( actionObj instanceof COSArray )
        {
            action = PDDestination.create( actionObj );
        }
        else
        {
            throw new IOException( "Unknown OpenAction " + actionObj );
        }

        return action;
    }
    /**
     * @return The Additional Actions for this Document
     */
    public PDDocumentCatalogAdditionalActions getActions()
    {
        COSDictionary addAct = (COSDictionary) root.getDictionaryObject("AA");
        if (addAct == null)
        {
            addAct = new COSDictionary();
            root.setItem("AA", addAct);
        }
        return new PDDocumentCatalogAdditionalActions(addAct);
    }

    /**
     * Set the additional actions for the document.
     *
     * @param actions The actions that are associated with this document.
     */
    public void setActions( PDDocumentCatalogAdditionalActions actions )
    {
        root.setItem("AA", actions );
    }

    /**
     * @return The names dictionary for this document or null if none exist.
     */
    public PDDocumentNameDictionary getNames()
    {
        PDDocumentNameDictionary nameDic = null;
        COSDictionary names = (COSDictionary) root.getDictionaryObject("Names");
        if(names != null)
        {
            nameDic = new PDDocumentNameDictionary(this,names);
        }
        return nameDic;
    }

    /**
     * Set the names dictionary for the document.
     *
     * @param names The names dictionary that is associated with this document.
     */
    public void setNames( PDDocumentNameDictionary names )
    {
        root.setItem("Names", names );
    }

    /**
     * Get info about doc's usage of tagged features.  This will return
     * null if there is no information.
     *
     * @return The new mark info.
     */
    public PDMarkInfo getMarkInfo()
    {
        PDMarkInfo retval = null;
        COSDictionary dic = (COSDictionary)root.getDictionaryObject( "MarkInfo" );
        if( dic != null )
        {
            retval = new PDMarkInfo( dic );
        }
        return retval;
    }

    /**
     * Set information about the doc's usage of tagged features.
     *
     * @param markInfo The new MarkInfo data.
     */
    public void setMarkInfo( PDMarkInfo markInfo )
    {
        root.setItem( "MarkInfo", markInfo );
    }

    /**
     * Set the page display mode, see the PAGE_MODE_XXX constants.
     * @return A string representing the page mode.
     */
    public String getPageMode()
    {
        return root.getNameAsString( "PageMode", PAGE_MODE_USE_NONE );
    }

    /**
     * Set the page mode.  See the PAGE_MODE_XXX constants for valid values.
     * @param mode The new page mode.
     */
    public void setPageMode( String mode )
    {
        root.setName( "PageMode", mode );
    }

    /**
     * Set the page layout, see the PAGE_LAYOUT_XXX constants.
     * @return A string representing the page layout.
     */
    public String getPageLayout()
    {
        return root.getNameAsString( "PageLayout", PAGE_LAYOUT_SINGLE_PAGE );
    }

    /**
     * Set the page layout.  See the PAGE_LAYOUT_XXX constants for valid values.
     * @param layout The new page layout.
     */
    public void setPageLayout( String layout )
    {
        root.setName( "PageLayout", layout );
    }

    /**
     * Document level information in the URI.
     * @return Document level URI.
     */
    public PDURIDictionary getURI()
    {
        PDURIDictionary retval = null;
        COSDictionary uri = (COSDictionary)root.getDictionaryObject( "URI" );
        if( uri != null )
        {
            retval = new PDURIDictionary( uri );
        }
        return retval;
    }

    /**
     * Set the document level uri.
     * @param uri The new document level uri.
     */
    public void setURI( PDURIDictionary uri )
    {
        root.setItem( "URI", uri );
    }

    /**
     * Get the document's structure tree root.
     *
     * @return The document's structure tree root or null if none exists.
     */
    public PDStructureTreeRoot getStructureTreeRoot()
    {
        PDStructureTreeRoot treeRoot = null;
        COSDictionary dic = (COSDictionary)root.getDictionaryObject( "StructTreeRoot" );
        if( dic != null )
        {
            treeRoot = new PDStructureTreeRoot( dic );
        }
        return treeRoot;
    }

    /**
     * Set the document's structure tree root.
     *
     * @param treeRoot The new structure tree.
     */
    public void setStructureTreeRoot( PDStructureTreeRoot treeRoot )
    {
        root.setItem( "StructTreeRoot", treeRoot );
    }

    /**
     * The language for the document.
     *
     * @return The language for the document.
     */
    public String getLanguage()
    {
        return root.getString( "Lang" );
    }

    /**
     * Set the Language for the document.
     *
     * @param language The new document language.
     */
    public void setLanguage( String language )
    {
        root.setString( "Lang", language );
    }

    /**
     * Returns the page labels descriptor of the document.
     * 
     * @return the page labels descriptor of the document.
     * 
     * @throws IOException If there is a problem retrieving the page labels.
     */
    public PDPageLabels getPageLabels() throws IOException 
    {
        PDPageLabels labels = null;
        COSDictionary dict = (COSDictionary) root.getDictionaryObject("PageLabels");
        if (dict != null) 
        {
            labels = new PDPageLabels(document, dict);
        }
        return labels;
    }

    /**
     * Set the page label descriptor for the document.
     *
     * @param labels the new page label descriptor to set.
     */
    public void setPageLabels(PDPageLabels labels) 
    {
        root.setItem("PageLabels", labels);
    }

}