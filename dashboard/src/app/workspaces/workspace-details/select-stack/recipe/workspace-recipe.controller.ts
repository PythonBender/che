/*
 * Copyright (c) 2015-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 */
'use strict';

/**
 * @ngdoc controller
 * @name workspaces.recipe.controller:WorkspacesRecipeCtrl
 * @description This class is handling the controller for the workspace recipe widget
 * @author Oleksii Orel
 */
export class WorkspaceRecipeController {
  $timeout: ng.ITimeoutService;

  editingTimeoutPromise: ng.IPromise<any>;

  recipeUrl: string;
  recipeFormat: string;
  recipeScript: string;

  // default selection
  selectSourceOption: string = 'upload-custom-stack';

  editorOptions: {
    lineWrapping: boolean,
    lineNumbers: boolean,
    matchBrackets: boolean,
    mode: string,
    onLoad: Function
  };

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor($timeout: ng.ITimeoutService) {
    this.$timeout = $timeout;

    this.setDefaultData();

    this.editorOptions = {
      lineWrapping: true,
      lineNumbers: true,
      matchBrackets: true,
      mode: this.recipeFormat,
      onLoad: (editor: any) => {
        this.setEditor(editor);
      }
    };
  }

  setDefaultData(): void {
    this.recipeUrl = null;
    this.recipeScript = '';
    this.recipeFormat = 'compose';
  }

  setEditor(editor: any): void {
    editor.on('paste', () => {
      this.detectFormat(editor);
    });
    editor.on('change', () => {
      this.trackChangesInProgress(editor);
    });
  }

  trackChangesInProgress(editor: any): void {
    if (this.editingTimeoutPromise) {
      this.$timeout.cancel(this.editingTimeoutPromise);
    }

    this.editingTimeoutPromise = this.$timeout(() => {
      this.detectFormat(editor);
    }, 1000);
  }

  detectFormat(editor: any): void {
    let content = editor.getValue();

    // compose format detection:
    if (content.match(/^services:\n/m)) {
      this.recipeFormat = 'compose';
      this.editorOptions.mode = 'text/x-yaml';
    }

    // docker file format detection
    if (content.match(/^FROM\s+\w+/m)) {
      this.recipeFormat = 'dockerfile';
      this.editorOptions.mode = 'text/x-dockerfile';
    }
  }
}
