"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _visitorKeys = require("./generated/visitor-keys");

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

/**
 * The base class for transforming the Hermes AST to the desired output format.
 * Extended by concrete adapters which output an ESTree or Babel AST.
 */
var HermesASTAdapter = /*#__PURE__*/function () {
  function HermesASTAdapter(options) {
    _classCallCheck(this, HermesASTAdapter);

    _defineProperty(this, "sourceFilename", void 0);

    _defineProperty(this, "sourceType", void 0);

    this.sourceFilename = options.sourceFilename;
    this.sourceType = options.sourceType;
  }
  /**
   * Transform the input Hermes AST to the desired output format.
   * This modifies the input AST in place instead of constructing a new AST.
   */


  _createClass(HermesASTAdapter, [{
    key: "transform",
    value: function transform(program) {
      // Comments are not traversed via visitor keys
      var comments = program.comments;

      for (var i = 0; i < comments.length; i++) {
        var comment = comments[i];
        this.fixSourceLocation(comment);
        comments[i] = this.mapComment(comment);
      } // The first comment may be an interpreter directive and is stored directly on the program node


      program.interpreter = comments.length > 0 && comments[0].type === 'InterpreterDirective' ? comments.shift() : null; // Tokens are not traversed via visitor keys

      var tokens = program.tokens;

      if (tokens) {
        for (var _i = 0; _i < tokens.length; _i++) {
          this.fixSourceLocation(tokens[_i]);
        }
      }

      return this.mapNode(program);
    }
    /**
     * Transform a Hermes AST node to the output AST format.
     *
     * This may modify the input node in-place and return that same node, or a completely
     * new node may be constructed and returned. Overriden in child classes.
     */

  }, {
    key: "mapNode",
    value: function mapNode(_node) {
      throw new Error('Implemented in subclasses');
    }
  }, {
    key: "mapNodeDefault",
    value: function mapNodeDefault(node) {
      var visitorKeys = _visitorKeys.HERMES_AST_VISITOR_KEYS[node.type];

      for (var key in visitorKeys) {
        var childType = visitorKeys[key];

        if (childType === _visitorKeys.NODE_CHILD) {
          var child = node[key];

          if (child != null) {
            node[key] = this.mapNode(child);
          }
        } else if (childType === _visitorKeys.NODE_LIST_CHILD) {
          var children = node[key];

          for (var i = 0; i < children.length; i++) {
            var _child = children[i];

            if (_child != null) {
              children[i] = this.mapNode(_child);
            }
          }
        }
      }

      return node;
    }
    /**
     * Update the source location for this node depending on the output AST format.
     * This can modify the input node in-place. Overriden in child classes.
     */

  }, {
    key: "fixSourceLocation",
    value: function fixSourceLocation(_node) {
      throw new Error('Implemented in subclasses');
    }
  }, {
    key: "getSourceType",
    value: function getSourceType() {
      var _this$sourceType;

      return (_this$sourceType = this.sourceType) !== null && _this$sourceType !== void 0 ? _this$sourceType : 'script';
    }
  }, {
    key: "setModuleSourceType",
    value: function setModuleSourceType() {
      if (this.sourceType == null) {
        this.sourceType = 'module';
      }
    }
  }, {
    key: "mapComment",
    value: function mapComment(node) {
      return node;
    }
  }, {
    key: "mapEmpty",
    value: function mapEmpty(_node) {
      // $FlowExpectedError
      return null;
    }
  }, {
    key: "mapImportDeclaration",
    value: function mapImportDeclaration(node) {
      if (node.importKind === 'value') {
        this.setModuleSourceType();
      }

      return this.mapNodeDefault(node);
    }
  }, {
    key: "mapImportSpecifier",
    value: function mapImportSpecifier(node) {
      if (node.importKind === 'value') {
        node.importKind = null;
      }

      return this.mapNodeDefault(node);
    }
  }, {
    key: "mapExportDefaultDeclaration",
    value: function mapExportDefaultDeclaration(node) {
      this.setModuleSourceType();
      return this.mapNodeDefault(node);
    }
  }, {
    key: "mapExportNamedDeclaration",
    value: function mapExportNamedDeclaration(node) {
      if (node.exportKind === 'value') {
        this.setModuleSourceType();
      }

      return this.mapNodeDefault(node);
    }
  }, {
    key: "mapExportAllDeclaration",
    value: function mapExportAllDeclaration(node) {
      if (node.exportKind === 'value') {
        this.setModuleSourceType();
      }

      return this.mapNodeDefault(node);
    }
  }, {
    key: "mapPrivateProperty",
    value: function mapPrivateProperty(node) {
      throw new SyntaxError(this.formatError(node, 'Private properties are not supported'));
    }
  }, {
    key: "formatError",
    value: function formatError(node, message) {
      return "".concat(message, " (").concat(node.loc.start.line, ":").concat(node.loc.start.column, ")");
    }
  }]);

  return HermesASTAdapter;
}();

exports["default"] = HermesASTAdapter;