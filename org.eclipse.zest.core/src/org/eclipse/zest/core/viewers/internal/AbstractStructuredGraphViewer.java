/*******************************************************************************
 * Copyright 2005-2006, CHISEL Group, University of Victoria, Victoria, BC,
 * Canada. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: The Chisel Group, University of Victoria
 ******************************************************************************/
package org.eclipse.mylar.zest.core.viewers.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.mylar.zest.core.ZestController;
import org.eclipse.mylar.zest.core.ZestException;
import org.eclipse.mylar.zest.core.ZestStyles;
import org.eclipse.mylar.zest.core.viewers.AbstractZoomableViewer;
import org.eclipse.mylar.zest.core.viewers.IGraphContentProvider;
import org.eclipse.mylar.zest.core.widgets.ConstraintAdapter;
import org.eclipse.mylar.zest.core.widgets.Graph;
import org.eclipse.mylar.zest.core.widgets.GraphConnection;
import org.eclipse.mylar.zest.core.widgets.GraphNode;
import org.eclipse.mylar.zest.core.widgets.IGraphConnection;
import org.eclipse.mylar.zest.core.widgets.IGraphItem;
import org.eclipse.mylar.zest.core.widgets.IGraphNode;
import org.eclipse.mylar.zest.core.widgets.IZestGraphDefaults;
import org.eclipse.mylar.zest.layouts.LayoutAlgorithm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Widget;

/**
 * Abstraction of graph viewers to implement functionality used by all of them.
 * Not intended to be implemented by clients. Use one of the provided children
 * instead.
 * 
 * @author Del Myers
 * 
 */
public abstract class AbstractStructuredGraphViewer extends AbstractZoomableViewer {
	/**
	 * Contains top-level styles for the entire graph. Set in the constructor. *
	 */
	private int graphStyle;

	/**
	 * Contains node-level styles for the graph. Set in setNodeStyle(). Defaults
	 * are used in the constructor.
	 */
	private int nodeStyle;

	/**
	 * Contains arc-level styles for the graph. Set in setConnectionStyle().
	 * Defaults are used in the constructor.
	 */
	private int connectionStyle;

	private HashMap nodesMap = new HashMap();
	private HashMap connectionsMap = new HashMap();

	/**
	 * The constraint adatpers
	 */
	private List constraintAdapters = new ArrayList();

	/**
	 * A simple graph comparator that orders graph elements based on thier type
	 * (connection or node), and their unique object identification.
	 */
	private class SimpleGraphComparator implements Comparator {
		TreeSet storedStrings;

		/**
		 * 
		 */
		public SimpleGraphComparator() {
			this.storedStrings = new TreeSet();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object arg0, Object arg1) {
			if (arg0 instanceof IGraphNode && arg1 instanceof IGraphConnection) {
				return 1;
			} else if (arg0 instanceof IGraphConnection && arg1 instanceof IGraphNode) {
				return -1;
			}
			if (arg0.equals(arg1)) {
				return 0;
			}
			return getObjectString(arg0).compareTo(getObjectString(arg1));
		}

		private String getObjectString(Object o) {
			String s = o.getClass().getName() + "@" + Integer.toHexString(o.hashCode());
			while (storedStrings.contains(s)) {
				s = s + 'X';
			}
			return s;
		}
	}

	protected AbstractStructuredGraphViewer(int graphStyle) {
		this.graphStyle = graphStyle;
		this.connectionStyle = IZestGraphDefaults.CONNECTION_STYLE;
		this.nodeStyle = IZestGraphDefaults.NODE_STYLE;

	}

	/**
	 * Sets the default style for nodes in this graph. Note: if an input is set
	 * on the viewer, a ZestException will be thrown.
	 * 
	 * @param nodeStyle
	 *            the style for the nodes.
	 * @see #ZestStyles
	 */
	public void setNodeStyle(int nodeStyle) {
		if (getInput() != null) {
			ZestController.error(ZestException.ERROR_CANNOT_SET_STYLE);
		}
		this.nodeStyle = nodeStyle;
	}

	/**
	 * Sets the default style for connections in this graph. Note: if an input
	 * is set on the viewer, a ZestException will be thrown.
	 * 
	 * @param connectionStyle
	 *            the style for the connections.
	 * @see #ZestStyles
	 */
	public void setConnectionStyle(int connectionStyle) {
		if (getInput() != null) {
			ZestController.error(ZestException.ERROR_CANNOT_SET_STYLE);
		}
		if (!ZestStyles.validateConnectionStyle(connectionStyle)) {
			ZestController.error(ZestException.ERROR_INVALID_STYLE);
		}
		this.connectionStyle = connectionStyle;
	}

	/**
	 * Returns the style set for the graph
	 * 
	 * @return The style set of the graph
	 */
	public int getGraphStyle() {
		return graphStyle;
	}

	/**
	 * Returns the style set for the nodes.
	 * 
	 * @return the style set for the nodes.
	 */
	public int getNodeStyle() {
		return nodeStyle;
	}

	public Graph getGraphControl() {
		return (Graph) getControl();
	}

	/**
	 * @return the connection style.
	 */
	public int getConnectionStyle() {
		return connectionStyle;
	}

	/**
	 * Adds a new constraint adapter to the list of constraints
	 * 
	 * @param constraintAdapter
	 */
	public void addConstraintAdapter(ConstraintAdapter constraintAdapter) {
		this.constraintAdapters.add(constraintAdapter);
	}

	/**
	 * Gets all the constraint adapters currently on the viewer
	 * 
	 * @return
	 */
	public List getConstraintAdapters() {
		return this.constraintAdapters;
	}

	/**
	 * Sets the layout algorithm for this viewer. Subclasses may place
	 * restrictions on the algorithms that it accepts.
	 * 
	 * @param algorithm
	 *            the layout algorithm
	 * @param run
	 *            true if the layout algorithm should be run immediately. This
	 *            is a hint.
	 */
	public abstract void setLayoutAlgorithm(LayoutAlgorithm algorithm, boolean run);

	/**
	 * Gets the current layout algorithm.
	 * 
	 * @return the current layout algorithm.
	 */
	protected abstract LayoutAlgorithm getLayoutAlgorithm();

	/**
	 * Equivalent to setLayoutAlgorithm(algorithm, false).
	 * 
	 * @param algorithm
	 */
	public void setLayoutAlgorithm(LayoutAlgorithm algorithm) {
		setLayoutAlgorithm(algorithm, false);
	}

	HashMap getNodesMap() {
		return this.nodesMap;
	}

	IGraphNode addGraphModelNode(Object element) {
		IGraphNode node = this.getGraphModelNode(element);
		if (node == null) {
			node = new GraphNode((Graph) getControl(), SWT.NONE);
			this.nodesMap.put(element, node);
			node.setData(element);
		}
		return node;
	}

	IGraphConnection addGraphModelConnection(Object element, IGraphNode source, IGraphNode target) {
		IGraphConnection connection = this.getGraphModelConnection(element);
		if (connection == null) {
			connection = new GraphConnection((Graph) getControl(), SWT.NONE, source, target);
			this.connectionsMap.put(element, connection);
			connection.setData(element);
		}
		return connection;

	}

	IGraphConnection getGraphModelConnection(Object obj) {
		return (IGraphConnection) this.connectionsMap.get(obj);
	}

	IGraphNode getGraphModelNode(Object obj) {
		return (IGraphNode) this.nodesMap.get(obj);
	}

	void removeGraphModelConnection(Object obj) {
		GraphConnection connection = (GraphConnection) connectionsMap.get(obj);
		if (connection != null) {
			connectionsMap.remove(obj);
			if (!connection.isDisposed()) {
				connection.dispose();
			}
		}
	}

	void removeGraphModelNode(Object obj) {
		GraphNode connection = (GraphNode) nodesMap.get(obj);
		if (connection != null) {
			nodesMap.remove(obj);
			if (!connection.isDisposed()) {
				connection.dispose();
			}
		}
	}

	protected void handleDispose(DisposeEvent event) {

		if (getControl() != null && !getControl().isDisposed()) {
			getControl().dispose();
		}
		super.handleDispose(event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#internalRefresh(java.lang.Object)
	 */
	protected void internalRefresh(Object element) {
		if (getInput() == null) {
			return;
		}
		if (element == getInput()) {
			getFactory().refreshGraph(getGraphControl());
		} else {
			getFactory().refresh(getGraphControl(), element);
		}
	}

	protected void doUpdateItem(Widget item, Object element, boolean fullMap) {
		if (item == getGraphControl()) {
			getFactory().update(getNodesArray(getGraphControl()));
			getFactory().update(getConnectionsArray(getGraphControl()));
		} else if (item instanceof IGraphItem) {
			getFactory().update((IGraphItem) item);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#doFindInputItem(java.lang.Object)
	 */
	protected Widget doFindInputItem(Object element) {
		if (element == getInput()) {
			return getGraphControl();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#doFindItem(java.lang.Object)
	 */
	protected Widget doFindItem(Object element) {
		Widget node = (Widget) nodesMap.get(element);
		Widget connection = (Widget) connectionsMap.get(element);
		return (node != null) ? node : connection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#getSelectionFromWidget()
	 */
	protected List getSelectionFromWidget() {
		List internalSelection = getWidgetSelection();
		LinkedList externalSelection = new LinkedList();
		for (Iterator i = internalSelection.iterator(); i.hasNext();) {
			// @tag zest.todo : should there be a method on IGraphItem to get
			// the external data?
			IGraphItem item = (IGraphItem) i.next();
			if (item instanceof IGraphNode) {
				externalSelection.add(((IGraphNode) item).getExternalNode());
			} else if (item instanceof IGraphConnection) {
				externalSelection.add(((IGraphConnection) item).getExternalConnection());
			} else if (item instanceof Widget) {
				externalSelection.add(((Widget) item).getData());
			}
		}
		return externalSelection;
	}

	protected IGraphItem[] /* GraphItem */findItems(List l) {
		ArrayList list = new ArrayList();
		Iterator iterator = l.iterator();

		while (iterator.hasNext()) {
			IGraphItem w = (IGraphItem) findItem(iterator.next());
			list.add(w);
		}
		return (IGraphItem[]) list.toArray(new IGraphItem[list.size()]);
	}

	//	protected List /* GraphItem */getWidgets(List l) {
	//		ArrayList list = new ArrayList();
	//		Iterator iterator = l.iterator();
	//		while (iterator.hasNext()) {
	//			Object obj = iterator.next();
	//			GraphItem item = (GraphNode) nodesMap.get(obj);
	//			if (item == null) {
	//				item = (GraphItem) connectionsMap.get(obj);
	//			}
	//			if (item != null) {
	//				list.add(item);
	//			}
	//		}
	//		return list;
	//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#setSelectionToWidget(java.util.List,
	 *      boolean)
	 */
	protected void setSelectionToWidget(List l, boolean reveal) {
		Graph control = (Graph) getControl();
		List selection = new LinkedList();
		for (Iterator i = l.iterator(); i.hasNext();) {
			Object obj = i.next();
			IGraphNode node = (IGraphNode) nodesMap.get(obj);
			IGraphConnection conn = (IGraphConnection) connectionsMap.get(obj);
			if (node != null) {
				selection.add(node);
			}
			if (conn != null) {
				selection.add(conn);
			}
		}
		control.setSelection((GraphNode[]) selection.toArray(new GraphNode[selection.size()]));
	}

	/**
	 * Gets the internal model elements that are selected.
	 * 
	 * @return
	 */
	protected List getWidgetSelection() {
		Graph control = (Graph) getControl();
		return control.getSelection();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.Viewer#inputChanged(java.lang.Object,
	 *      java.lang.Object)
	 */
	protected void inputChanged(Object input, Object oldInput) {
		IStylingGraphModelFactory factory = getFactory();
		factory.setConnectionStyle(getConnectionStyle());
		factory.setNodeStyle(getNodeStyle());

		// Save the old map so we can set the size and possition of any nodes that are the same
		Map oldNodesMap = nodesMap;
		Graph graph = (Graph) getControl();
		graph.setSelection(new GraphNode[0]);

		Iterator iterator = nodesMap.values().iterator();
		while (iterator.hasNext()) {
			IGraphNode node = (IGraphNode) iterator.next();
			if (!node.isDisposed()) {
				node.dispose();
			}
		}

		iterator = connectionsMap.values().iterator();
		while (iterator.hasNext()) {
			IGraphConnection connection = (IGraphConnection) iterator.next();
			if (!connection.isDisposed()) {
				connection.dispose();
			}
		}

		nodesMap = new HashMap();
		connectionsMap = new HashMap();

		graph = factory.createGraphModel(graph);

		((Graph) getControl()).setNodeStyle(getNodeStyle());
		((Graph) getControl()).setConnectionStyle(getConnectionStyle());

		// check if any of the pre-existing nodes are still present
		// in this case we want them to keep the same location & size
		for (Iterator iter = oldNodesMap.keySet().iterator(); iter.hasNext();) {
			Object data = iter.next();
			IGraphNode newNode = (IGraphNode) nodesMap.get(data);
			if (newNode != null) {
				IGraphNode oldNode = (IGraphNode) oldNodesMap.get(data);
				newNode.setPreferredLocation(oldNode.getXInLayout(), oldNode.getYInLayout());
				newNode.setSizeInLayout(oldNode.getWidthInLayout(), oldNode.getHeightInLayout());
			}
		}

		applyLayout();
	}

	/**
	 * Returns the factory used to create the model. This must not be called
	 * before the content provider is set.
	 * 
	 * @return
	 */
	protected abstract IStylingGraphModelFactory getFactory();

	protected void filterVisuals() {
		if (getGraphControl() == null) {
			return;
		}
		Object[] filtered = getFilteredChildren(getInput());
		SimpleGraphComparator comparator = new SimpleGraphComparator();
		TreeSet filteredElements = new TreeSet(comparator);
		TreeSet unfilteredElements = new TreeSet(comparator);
		List connections = getGraphControl().getConnections();
		List nodes = getGraphControl().getNodes();
		if (filtered.length == 0) {
			// set everything to invisible.
			// @tag zest.bug.156528-Filters.check : should we only filter out
			// the nodes?
			for (Iterator i = connections.iterator(); i.hasNext();) {
				IGraphConnection c = (IGraphConnection) i.next();
				c.setVisible(false);
			}
			for (Iterator i = nodes.iterator(); i.hasNext();) {
				IGraphNode n = (IGraphNode) i.next();
				n.setVisible(false);
			}
			return;
		}
		for (Iterator i = connections.iterator(); i.hasNext();) {
			IGraphConnection c = (IGraphConnection) i.next();
			if (c.getExternalConnection() != null) {
				unfilteredElements.add(c);
			}
		}
		for (Iterator i = nodes.iterator(); i.hasNext();) {
			IGraphNode n = (IGraphNode) i.next();
			if (n.getExternalNode() != null) {
				unfilteredElements.add(n);
			}
		}
		for (int i = 0; i < filtered.length; i++) {
			Object modelElement = connectionsMap.get(filtered[i]);
			if (modelElement == null) {
				modelElement = nodesMap.get(filtered[i]);
			}
			if (modelElement != null) {
				filteredElements.add(modelElement);
			}
		}
		unfilteredElements.removeAll(filteredElements);
		// set all the elements that did not pass the filters to invisible, and
		// all the elements that passed to visible.
		while (unfilteredElements.size() > 0) {
			IGraphItem i = (IGraphItem) unfilteredElements.first();
			i.setVisible(false);
			unfilteredElements.remove(i);
		}
		while (filteredElements.size() > 0) {
			IGraphItem i = (IGraphItem) filteredElements.first();
			i.setVisible(true);
			filteredElements.remove(i);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#getRawChildren(java.lang.Object)
	 */
	protected Object[] getRawChildren(Object parent) {
		if (parent == getInput()) {
			// get the children from the model.
			LinkedList children = new LinkedList();
			if (getGraphControl() != null) {
				List connections = getGraphControl().getConnections();
				List nodes = getGraphControl().getNodes();
				for (Iterator i = connections.iterator(); i.hasNext();) {
					IGraphConnection c = (IGraphConnection) i.next();
					if (c.getExternalConnection() != null) {
						children.add(c.getExternalConnection());
					}
				}
				for (Iterator i = nodes.iterator(); i.hasNext();) {
					IGraphNode n = (IGraphNode) i.next();
					if (n.getExternalNode() != null) {
						children.add(n.getExternalNode());
					}
				}
				return children.toArray();
			}
		}
		return super.getRawChildren(parent);
	}

	/**
	 * 
	 */
	public void reveal(Object element) {
		Widget[] items = this.findItems(element);
		for (int i = 0; i < items.length; i++) {
			Widget item = items[i];
			if (item instanceof GraphNode) {
				GraphNode graphModelNode = (GraphNode) item;
				graphModelNode.highlight();
			} else if (item instanceof GraphConnection) {
				GraphConnection graphModelConnection = (GraphConnection) item;
				graphModelConnection.highlight();
			}
		}
	}

	public void unReveal(Object element) {
		Widget[] items = this.findItems(element);
		for (int i = 0; i < items.length; i++) {
			Widget item = items[i];
			if (item instanceof GraphNode) {
				GraphNode graphModelNode = (GraphNode) item;
				graphModelNode.unhighlight();
			} else if (item instanceof GraphConnection) {
				GraphConnection graphModelConnection = (GraphConnection) item;
				graphModelConnection.unhighlight();
			}
		}
	}

	/**
	 * Applies the viewers layouts.
	 * 
	 */
	public abstract void applyLayout();

	/**
	 * Removes the given connection object from the layout algorithm and the
	 * model.
	 * 
	 * @param connection
	 */
	public void removeRelationship(Object connection) {
		IGraphConnection relationship = (IGraphConnection) connectionsMap.get(connection);

		if (relationship != null) {
			// remove the relationship from the layout algorithm
			if (getLayoutAlgorithm() != null) {
				getLayoutAlgorithm().removeRelationship(relationship);
			}
			// remove the relationship from the model
			relationship.dispose();
			applyLayout();
		}
	}

	/**
	 * Creates a new node and adds it to the graph. If it already exists nothing
	 * happens.
	 * 
	 * @param newNode
	 */
	public void addNode(Object element) {
		if (nodesMap.get(element) == null) {
			// create the new node
			IGraphNode newNode = getFactory().createNode(getGraphControl(), element);

			// add it to the layout algorithm
			if (getLayoutAlgorithm() != null) {
				getLayoutAlgorithm().addEntity(newNode);
			}
			applyLayout();
		}
	}

	/**
	 * Removes the given element from the layout algorithm and the model.
	 * 
	 * @param element
	 *            The node element to remove.
	 */
	public void removeNode(Object element) {
		IGraphNode node = (IGraphNode) nodesMap.get(element);

		if (node != null) {
			// remove the node from the layout algorithm and all the connections
			if (getLayoutAlgorithm() != null) {
				getLayoutAlgorithm().removeEntity(node);
				getLayoutAlgorithm().removeRelationships(node.getSourceConnections());
				getLayoutAlgorithm().removeRelationships(node.getTargetConnections());
			}

			// remove the node and it's connections from the model
			node.dispose();
			applyLayout();
		}
	}

	/**
	 * Creates a new relationship between the source node and the destination
	 * node. If either node doesn't exist then it will be created.
	 * 
	 * @param connection
	 *            The connection data object.
	 * @param srcNode
	 *            The source node data object.
	 * @param destNode
	 *            The destination node data object.
	 */
	public void addRelationship(Object connection, Object srcNode, Object destNode) {
		// create the new relationship
		IStylingGraphModelFactory modelFactory = getFactory();
		IGraphConnection newConnection = modelFactory.createConnection(getGraphControl(), connection, srcNode, destNode);

		// add it to the layout algorithm
		if (getLayoutAlgorithm() != null) {
			getLayoutAlgorithm().addRelationship(newConnection);
		}
		applyLayout();
	}

	/**
	 * Adds a new relationship given the connection. It will use the content
	 * provider to determine the source and destination nodes.
	 * 
	 * @param connection
	 *            The connection data object.
	 */
	public void addRelationship(Object connection) {
		IStylingGraphModelFactory modelFactory = getFactory();
		if (connectionsMap.get(connection) == null) {
			if (modelFactory.getContentProvider() instanceof IGraphContentProvider) {
				IGraphContentProvider content = ((IGraphContentProvider) modelFactory.getContentProvider());
				Object source = content.getSource(connection);
				Object dest = content.getDestination(connection);
				// create the new relationship
				IGraphConnection newConnection = modelFactory.createConnection(getGraphControl(), connection, source, dest);
				// add it to the layout algorithm
				if (getLayoutAlgorithm() != null) {
					getLayoutAlgorithm().addRelationship(newConnection);
				}
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	/**
	 * Converts the list of GraphModelConnection objects into an array and
	 * returns it.
	 * 
	 * @return GraphModelConnection[]
	 */
	protected IGraphConnection[] getConnectionsArray(Graph graph) {
		IGraphConnection[] connsArray = new IGraphConnection[graph.getConnections().size()];
		connsArray = (IGraphConnection[]) graph.getConnections().toArray(connsArray);
		return connsArray;
	}

	/**
	 * Converts the list of GraphModelNode objects into an array an returns it.
	 * 
	 * @return GraphModelNode[]
	 */
	protected IGraphNode[] getNodesArray(Graph graph) {
		IGraphNode[] nodesArray = new IGraphNode[graph.getNodes().size()];
		nodesArray = (IGraphNode[]) graph.getNodes().toArray(nodesArray);
		return nodesArray;
	}

}