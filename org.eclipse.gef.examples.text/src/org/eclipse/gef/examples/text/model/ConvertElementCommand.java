/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/ 

package org.eclipse.gef.examples.text.model;

import org.eclipse.gef.examples.text.GraphicalTextViewer;
import org.eclipse.gef.examples.text.SelectionRange;

/**
 * @since 3.1
 */
public class ConvertElementCommand extends ExampleTextCommand {

private final TextRun text;
private final char removed[];
private final int offset;
private final ModelElement converted;
private final ModelLocation caret;

/**
 * 
 * @since 3.1
 */
public ConvertElementCommand(TextRun text, int offset, ModelElement converted, ModelLocation caret) {
	super("bogus");
	this.text = text;
	this.offset = offset;
	this.converted = converted;
	this.caret = caret;
	removed = text.getText().substring(offset).toCharArray();	
}

public void execute() {
	text.removeRange(offset, removed.length);
	Container container = text.getContainer();
	container.remove(text);
	container.add(converted);
}

public SelectionRange getRedoSelectionRange(GraphicalTextViewer viewer) {
	return null;
}

public SelectionRange getExecuteSelectionRange(GraphicalTextViewer viewer) {
	return new SelectionRange(lookupModel(viewer, caret.model), caret.offset);
}

public SelectionRange getUndoSelectionRange(GraphicalTextViewer viewer) {
	return null;
}

}
