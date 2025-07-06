import {
  Decoration,
  DecorationManager,
  DecorationTemplate,
} from "../common/decoration"

export class DecorationWrapperParentSide {
  private messagePort?: MessagePort

  setMessagePort(messagePort: MessagePort) {
    this.messagePort = messagePort
  }

  registerTemplates(templates: Map<string, DecorationTemplate>) {
    this.send({ kind: "registerTemplates", templates })
  }

  addDecoration(decoration: Decoration, group: string) {
    this.send({ kind: "addDecoration", decoration, group })
  }

  removeDecoration(id: string, group: string) {
    this.send({ kind: "removeDecoration", id, group })
  }

  private send(message: DecorationCommand) {
    this.messagePort?.postMessage(message)
  }
}

export class DecorationWrapperIframeSide {
  private readonly decorationManager: DecorationManager

  constructor(messagePort: MessagePort, decorationManager: DecorationManager) {
    this.decorationManager = decorationManager

    messagePort.onmessage = (message) => {
      this.onCommand(message.data as DecorationCommand)
    }
  }

  private onCommand(command: DecorationCommand) {
    switch (command.kind) {
      case "registerTemplates":
        return this.registerTemplates(command.templates)
      case "addDecoration":
        return this.addDecoration(command.decoration, command.group)
      case "removeDecoration":
        return this.removeDecoration(command.id, command.group)
    }
  }

  private registerTemplates(templates: Map<string, DecorationTemplate>) {
    this.decorationManager.registerTemplates(templates)
  }

  private addDecoration(decoration: Decoration, group: string) {
    this.decorationManager.addDecoration(decoration, group)
  }

  private removeDecoration(id: string, group: string) {
    this.decorationManager.removeDecoration(id, group)
  }
}

interface AddDecoration {
  kind: "addDecoration"
  decoration: Decoration
  group: string
}

interface RemoveDecoration {
  kind: "removeDecoration"
  id: string
  group: string
}

interface RegisterTemplates {
  kind: "registerTemplates"
  templates: Map<string, DecorationTemplate>
}

type DecorationCommand = AddDecoration | RemoveDecoration | RegisterTemplates
