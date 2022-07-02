package com.sdercolin.vlabeler.ui.string

import com.sdercolin.vlabeler.ui.string.Language.English

enum class Strings(val en: String) {
    AppName(
        en = "vLabeler"
    ),
    LanguageDisplayName(
        en = English.displayName
    ),
    MenuFile(
        en = "File"
    ),
    MenuFileNewProject(
        en = "New Project..."
    ),
    MenuFileOpen(
        en = "Open..."
    ),
    MenuFileOpenRecent(
        en = "Open Recent"
    ),
    MenuFileOpenRecentClear(
        en = "Clear Recently Opened"
    ),
    MenuFileSave(
        en = "Save"
    ),
    MenuFileSaveAs(
        en = "Save As..."
    ),
    MenuFileExport(
        en = "Export..."
    ),
    MenuFileClose(
        en = "Close"
    ),
    MenuEdit(
        en = "Edit"
    ),
    MenuEditUndo(
        en = "Undo"
    ),
    MenuEditRedo(
        en = "Redo"
    ),
    MenuEditRenameEntry(
        en = "Rename Current Entry..."
    ),
    MenuEditDuplicateEntry(
        en = "Duplicate Current Entry..."
    ),
    MenuEditRemoveEntry(
        en = "Remove Current Entry"
    ),
    MenuView(
        en = "View"
    ),
    MenuViewToggleMarker(
        en = "Show Parameter Controllers"
    ),
    MenuViewPinEntryList(
        en = "Pin Entry List"
    ),
    MenuViewToggleProperties(
        en = "Show Properties"
    ),
    MenuNavigate(
        en = "Navigate"
    ),
    MenuNavigateNextEntry(
        en = "Go to Next Entry"
    ),
    MenuNavigatePreviousEntry(
        en = "Go to Previous Entry"
    ),
    MenuNavigateNextSample(
        en = "Go to Next Sample"
    ),
    MenuNavigatePreviousSample(
        en = "Go to Previous Sample"
    ),
    MenuNavigateJumpToEntry(
        en = "Go to Entry..."
    ),
    MenuNavigateScrollFit(
        en = "Scroll to Editable Area"
    ),
    CommonOkay(
        en = "OK"
    ),
    CommonCancel(
        en = "Cancel"
    ),
    CommonYes(
        en = "Yes"
    ),
    CommonNo(
        en = "No"
    ),
    StarterStart(
        en = "Start"
    ),
    StarterNewProject(
        en = "New Project..."
    ),
    StarterOpen(
        en = "Open..."
    ),
    StarterRecent(
        en = "Recent"
    ),
    StarterRecentEmpty(
        en = "You have no recent projects."
    ),
    StarterRecentDeleted(
        en = "This profile has been removed."
    ),
    StarterNewSampleDirectory(
        en = "Sample directory"
    ),
    StarterNewWorkingDirectory(
        en = "Working directory"
    ),
    StarterNewProjectName(
        en = "Project name"
    ),
    StarterNewProjectNameWarning(
        en = "The project file already exists. Creating project will overwrite the file."
    ),
    StarterNewLabeler(
        en = "Labeler"
    ),
    StarterNewTemplatePlugin(
        en = "Template generator"
    ),
    StarterNewTemplatePluginNone(
        en = "None"
    ),
    StarterNewInputFile(
        en = "Input file (.%s)"
    ),
    StarterNewInputFilePlaceholder(
        en = "(One would be created if left blank)"
    ),
    StarterNewInputFileDisabled(
        en = "No input file is required by the selected template generator"
    ),
    StarterNewEncoding(
        en = "Encoding"
    ),
    ChooseSampleDirectoryDialogTitle(
        en = "Choose sample directory"
    ),
    ChooseWorkingDirectoryDialogTitle(
        en = "Choose working directory"
    ),
    ChooseInputFileDialogTitle(
        en = "Choose input file"
    ),
    OpenProjectDialogTitle(
        en = "Open project"
    ),
    SaveAsProjectDialogTitle(
        en = "Save as project"
    ),
    ExportDialogTitle(
        en = "Export"
    ),
    SetResolutionDialogDescription(
        en = "Input horizontal resolution (points per pixel) for the editor (%d ~ %d)"
    ),
    AskIfSaveBeforeOpenDialogDescription(
        en = "You have unsaved changes. Do you want to save them before opening a new project?"
    ),
    AskIfSaveBeforeExportDialogDescription(
        en = "You have unsaved changes. Do you want to save them before exporting?"
    ),
    AskIfSaveBeforeCloseDialogDescription(
        en = "You have unsaved changes. Do you want to save them before closing the current project?"
    ),
    AskIfSaveBeforeExitDialogDescription(
        en = "You have unsaved changes. Do you want to save them before exiting?"
    ),
    EditEntryNameDialogDescription(
        en = "Rename this entry"
    ),
    EditEntryNameDuplicateDialogDescription(
        en = "Input the name of the new entry"
    ),
    EditEntryNameDialogExistingError(
        en = "The name you input already exists."
    ),
    AskIfRemoveEntryDialogDescription(
        en = "Removing current entry..."
    ),
    AskIfLoadAutoSavedProjectDialogDescription(
        en = "Auto-saved project file found. Do you want to load it? If not, the file will be discarded."
    ),
    CanvasLengthOverflowError(
        en = "The charts are too long to be painted." +
            " Please use \"-\" button to reduce data length, or divide the input files priorly."
    ),
    EmptySampleDirectoryException(
        en = "Could not create project because no sample files are found in the given sample directory."
    );

    fun get(language: Language): String = when (language) {
        English -> en
    }
}

fun string(key: Strings, vararg params: Any?): String {
    val template = key.get(currentLanguage)
    return template.format(*params)
}
