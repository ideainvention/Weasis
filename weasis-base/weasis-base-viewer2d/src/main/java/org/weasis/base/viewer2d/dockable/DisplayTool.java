/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.base.viewer2d.dockable;

import bibliothek.gui.dock.common.CLocation;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingEvent;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingModel.CheckingMode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.weasis.base.viewer2d.EventManager;
import org.weasis.base.viewer2d.Messages;
import org.weasis.base.viewer2d.View2dContainer;
import org.weasis.core.api.gui.Insertable;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.api.media.data.Thumbnailable;
import org.weasis.core.api.util.ResourceUtil;
import org.weasis.core.api.util.ResourceUtil.OtherIcon;
import org.weasis.core.ui.docking.PluginTool;
import org.weasis.core.ui.editor.SeriesViewerEvent;
import org.weasis.core.ui.editor.SeriesViewerEvent.EVENT;
import org.weasis.core.ui.editor.SeriesViewerListener;
import org.weasis.core.ui.editor.image.ImageViewerPlugin;
import org.weasis.core.ui.editor.image.Panner;
import org.weasis.core.ui.editor.image.ViewCanvas;
import org.weasis.core.ui.model.layer.LayerAnnotation;
import org.weasis.core.ui.util.CheckBoxTreeBuilder;

public class DisplayTool extends PluginTool implements SeriesViewerListener {

  public static final String BUTTON_NAME = Messages.getString("DisplayTool.display");

  private final JCheckBox applyAllViews =
      new JCheckBox(Messages.getString("apply.to.all.views"), true);
  private final CheckboxTree tree;
  private boolean initPathSelection;
  private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("rootNode", true);

  private DefaultMutableTreeNode image;
  private DefaultMutableTreeNode info;
  private DefaultMutableTreeNode drawings;
  private JPanel panelFoot;

  public DisplayTool(String pluginName) {
    super(BUTTON_NAME, pluginName, Insertable.Type.TOOL, 10);
    dockable.setTitleIcon(ResourceUtil.getIcon(OtherIcon.VIEW_SETTING));
    setDockableWidth(230);

    tree = new CheckboxTree();
    initPathSelection = false;
    setLayout(new BorderLayout(0, 0));
    iniTree();
  }

  public void iniTree() {
    tree.getCheckingModel().setCheckingMode(CheckingMode.SIMPLE);

    image = new DefaultMutableTreeNode(Messages.getString("DisplayTool.img"), true);
    rootNode.add(image);
    info = new DefaultMutableTreeNode(Messages.getString("DisplayTool.annotations"), true);
    info.add(new DefaultMutableTreeNode(LayerAnnotation.ANNOTATIONS, true));
    info.add(new DefaultMutableTreeNode(LayerAnnotation.SCALE, true));
    info.add(new DefaultMutableTreeNode(LayerAnnotation.LUT, true));
    info.add(new DefaultMutableTreeNode(LayerAnnotation.IMAGE_ORIENTATION, true));
    info.add(new DefaultMutableTreeNode(LayerAnnotation.WINDOW_LEVEL, true));
    info.add(new DefaultMutableTreeNode(LayerAnnotation.ZOOM, true));
    info.add(new DefaultMutableTreeNode(LayerAnnotation.ROTATION, true));
    info.add(new DefaultMutableTreeNode(LayerAnnotation.FRAME, true));
    info.add(new DefaultMutableTreeNode(LayerAnnotation.PIXEL, true));
    rootNode.add(info);
    drawings = new DefaultMutableTreeNode(ActionW.DRAWINGS, true);
    rootNode.add(drawings);

    DefaultTreeModel model = new DefaultTreeModel(rootNode, false);
    tree.setModel(model);
    TreePath rootPath = new TreePath(rootNode.getPath());
    tree.addCheckingPath(rootPath);

    tree.setShowsRootHandles(true);
    tree.setRootVisible(false);
    tree.setExpandsSelectedPaths(true);
    tree.setCellRenderer(CheckBoxTreeBuilder.buildNoIconCheckboxTreeCellRenderer());
    tree.addTreeCheckingListener(this::treeValueChanged);

    JPanel panel = new JPanel();
    FlowLayout flowLayout = (FlowLayout) panel.getLayout();
    flowLayout.setAlignment(FlowLayout.LEFT);
    add(panel, BorderLayout.NORTH);
    panel.add(applyAllViews);

    expandTree(tree, rootNode);
    add(new JScrollPane(tree), BorderLayout.CENTER);

    panelFoot = new JPanel();
    add(panelFoot, BorderLayout.SOUTH);
  }

  private void initPathSelection(TreePath path, boolean selected) {
    if (selected) {
      tree.addCheckingPath(path);
    } else {
      tree.removeCheckingPath(path);
    }
  }

  public void iniTreeValues(ViewCanvas<?> view) {
    if (view != null) {
      initPathSelection = true;
      // Image node
      initPathSelection(getTreePath(image), view.getImageLayer().getVisible());

      // Annotations node
      LayerAnnotation layer = view.getInfoLayer();
      if (layer != null) {
        initPathSelection(getTreePath(info), layer.getVisible());
        Enumeration<?> en = info.children();
        while (en.hasMoreElements()) {
          Object node = en.nextElement();
          if (node instanceof TreeNode checkNode) {
            initPathSelection(getTreePath(checkNode), layer.getDisplayPreferences(node.toString()));
          }
        }
      }

      ImageElement img = view.getImage();
      if (img != null) {
        Panner<?> panner = view.getPanner();
        if (panner != null) {
          int cps = panelFoot.getComponentCount();
          if (cps > 0) {
            Component cp = panelFoot.getComponent(0);
            if (cp != panner) {
              if (cp instanceof Thumbnailable thumbnailable) {
                thumbnailable.removeMouseAndKeyListener();
              }
              panner.registerListeners();
              panelFoot.removeAll();
              panelFoot.add(panner);
              panner.revalidate();
              panner.repaint();
            }
          } else {
            panner.registerListeners();
            panelFoot.add(panner);
          }
        }
      }

      initPathSelection = false;
    }
  }

  private void treeValueChanged(TreeCheckingEvent e) {
    if (!initPathSelection) {
      TreePath path = e.getPath();
      boolean selected = e.isCheckedPath();
      Object selObject = path.getLastPathComponent();
      Object parent = null;
      if (path.getParentPath() != null) {
        parent = path.getParentPath().getLastPathComponent();
      }

      ImageViewerPlugin<ImageElement> container =
          EventManager.getInstance().getSelectedView2dContainer();
      List<ViewCanvas<ImageElement>> views = null;
      if (container != null) {
        if (applyAllViews.isSelected()) {
          views = container.getImagePanels();
        } else {
          views = new ArrayList<>(1);
          Optional.ofNullable(container.getSelectedImagePane()).ifPresent(views::add);
        }
      }
      if (views == null) {
        return;
      }

      if (rootNode.equals(parent)) {
        if (image.equals(selObject)) {
          for (ViewCanvas<ImageElement> v : views) {
            if (selected != v.getImageLayer().getVisible()) {
              v.getImageLayer().setVisible(selected);
              v.getJComponent().repaint();
            }
          }
        } else if (info.equals(selObject)) {
          for (ViewCanvas<ImageElement> v : views) {
            if (selected != v.getInfoLayer().getVisible()) {
              v.getInfoLayer().setVisible(selected);
              v.getJComponent().repaint();
            }
          }
        } else if (drawings.equals(selObject)) {
          for (ViewCanvas<ImageElement> v : views) {
            v.setDrawingsVisibility(selected);
          }
        }

      } else if (info.equals(parent) && selObject != null) {
        for (ViewCanvas<ImageElement> v : views) {
          LayerAnnotation layer = v.getInfoLayer();
          if (layer != null && layer.setDisplayPreferencesValue(selObject.toString(), selected)) {
            v.getJComponent().repaint();
          }
        }
      }
    }
  }

  private static TreePath getTreePath(TreeNode node) {
    List<TreeNode> list = new ArrayList<>();
    list.add(node);
    TreeNode parent = node;
    while (parent.getParent() != null) {
      parent = parent.getParent();
      list.add(parent);
    }
    Collections.reverse(list);
    return new TreePath(list.toArray(new TreeNode[0]));
  }

  @Override
  public Component getToolComponent() {
    return this;
  }

  @Override
  protected void changeToolWindowAnchor(CLocation clocation) {
    // Do noting
  }

  @Override
  public void changingViewContentEvent(SeriesViewerEvent event) {
    EVENT e = event.getEventType();
    if (EVENT.SELECT_VIEW.equals(e)
        && event.getSeriesViewer() instanceof View2dContainer container) {
      iniTreeValues(container.getSelectedImagePane());
    }
  }

  private static void expandTree(JTree tree, DefaultMutableTreeNode start) {
    Enumeration<?> children = start.children();
    while (children.hasMoreElements()) {
      Object child = children.nextElement();
      if (child instanceof DefaultMutableTreeNode dtm && !dtm.isLeaf()) {
        TreePath tp = new TreePath(dtm.getPath());
        tree.expandPath(tp);
        expandTree(tree, dtm);
      }
    }
  }
}
