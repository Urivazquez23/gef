package org.eclipse.gef.ui.parts;
/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 2001, 2002 - All Rights Reserved.
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 */

import java.util.*;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.*;

import org.eclipse.gef.*;
import org.eclipse.gef.editparts.*;
import org.eclipse.gef.tools.*;
import org.eclipse.gef.commands.*;

/**
 * Provides support for representation of any other graphical
 * EditPart. It contains Layers which are used to represent
 * specific type of visual information. The Layers are
 * (1) Primary - Used to hold the main EditPart's Figures.
 * (2) Connection - Used to hold the connections between EditParts.
 * (3) Handle - Takes care of holding handles for EditParts.
 * (4) Feedback - Shows feedback information for the EditParts.
 * 
 */
public class GraphicalRootEditPart
	extends AbstractGraphicalEditPart
	implements RootEditPart, LayerConstants, LayerManager
{

protected EditPart contents;
protected EditPartViewer viewer;
private LayeredPane innerLayers;

protected void createEditPolicies(){}

protected IFigure createFigure() {
	innerLayers = new LayeredPane();

	Layer layer = new Layer();
	layer.setLayoutManager(new StackLayout());
	innerLayers.add(layer, PRIMARY_LAYER);

	layer = new ConnectionLayer();
	layer.setPreferredSize(new Dimension(5,5));
	innerLayers.add(layer, CONNECTION_LAYER);

	layer = new Layer();
	layer.setPreferredSize(new Dimension(5,5));
	innerLayers.add(layer, HANDLE_LAYER);

	layer = new FeedbackLayer();
	layer.setPreferredSize(new Dimension(5,5));
	innerLayers.add(layer, FEEDBACK_LAYER);
	
	ScrollPane pane = new ScrollPane();
	pane.setViewport( new Viewport(true) );
	pane.setView( innerLayers );

	return pane;
}

/** 
 * Doesnt provide any command support, returns an
 * un-executable command
 */
public Command getCommand(Request req){
	return UnexecutableCommand.INSTANCE;
}

public EditPart getContents(){
	return contents;
}

/**
 * Return a drag tracker suitable for dragging this.
 */
public DragTracker getDragTracker(Request req) {
	// The drawing cannot be dragged.
	return new MarqueeDragTracker();
}

/**
 * Returns the layer for the given key
 */
public IFigure getLayer(Object key){
	if (innerLayers == null)
		return null;
	return innerLayers.getLayer(key);
}

/**
 * Returns the figure to which childrens' figures will be added.
 * An example would be a ScrollPane.  Figures of child editpart are not
 * added to the ScrollPane, but to its ViewPort's View.
 */
public IFigure getContentPane(){
	return getLayer(PRIMARY_LAYER);
}

/**
 * Returns the model of this EditPart. 
 */
public Object getModel(){
	return LayerManager.ID;
}

/**
 * Return this, as this is the root EditPart.
 * @return Root EditPart
 */
public RootEditPart getRoot() {return this;}

/**
 * Return the EditorView for this.
 * @param EditorView  The viewer for the Root.
 */
public EditPartViewer getViewer() {return viewer;}

public void refresh(){}

protected void refreshChildren(){}

/**
 * Sets the contents.  The root contains a single child, called it's contents.
 */
public void setContents(EditPart editpart){
	if (contents != null)
		removeChild(contents);
	contents = editpart;
	if (contents != null)
		addChild(contents,0);
}

/**
 * Sets the viewer.
 * @param viewer EditPartViewer.
 */
public void setViewer(EditPartViewer newViewer) {
	if (viewer == newViewer)
		return;
	if (viewer != null)
		unregister();
	viewer = newViewer;
	if (viewer != null)
		register();
}

class FeedbackLayer
	extends Layer
{
	public IFigure findFigureAtExcluding( int x, int y, Collection c ){
		return null;
	}
}

}
