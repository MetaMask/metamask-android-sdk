/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * 
 * @format
 */
'use strict';

function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.parse = parse;

var HermesParser = _interopRequireWildcard(require("./HermesParser"));

var _HermesToBabelAdapter = _interopRequireDefault(require("./HermesToBabelAdapter"));

var _HermesToESTreeAdapter = _interopRequireDefault(require("./HermesToESTreeAdapter"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _getRequireWildcardCache(nodeInterop) { if (typeof WeakMap !== "function") return null; var cacheBabelInterop = new WeakMap(); var cacheNodeInterop = new WeakMap(); return (_getRequireWildcardCache = function _getRequireWildcardCache(nodeInterop) { return nodeInterop ? cacheNodeInterop : cacheBabelInterop; })(nodeInterop); }

function _interopRequireWildcard(obj, nodeInterop) { if (!nodeInterop && obj && obj.__esModule) { return obj; } if (obj === null || _typeof(obj) !== "object" && typeof obj !== "function") { return { "default": obj }; } var cache = _getRequireWildcardCache(nodeInterop); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (key !== "default" && Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) { symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); } keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(Object(source), true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(Object(source)).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var DEFAULTS = {
  flow: 'detect'
};

function getOptions() {
  var options = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : _objectSpread({}, DEFAULTS);

  // Default to detecting whether to parse Flow syntax by the presence
  // of an  pragma.
  if (options.flow == null) {
    options.flow = DEFAULTS.flow;
  } else if (options.flow != 'all' && options.flow != 'detect') {
    throw new Error('flow option must be "all" or "detect"');
  }

  if (options.sourceType === 'unambiguous') {
    // Clear source type so that it will be detected from the contents of the file
    delete options.sourceType;
  } else if (options.sourceType != null && options.sourceType !== 'script' && options.sourceType !== 'module') {
    throw new Error('sourceType option must be "script", "module", or "unambiguous" if set');
  }

  options.tokens = options.tokens === true;
  options.allowReturnOutsideFunction = options.allowReturnOutsideFunction === true;
  return options;
}

function getAdapter(options, code) {
  return options.babel === true ? new _HermesToBabelAdapter["default"](options) : new _HermesToESTreeAdapter["default"](options, code);
} // $FlowExpectedError[unclear-type]


// eslint-disable-next-line no-redeclare
function parse(code, opts) {
  var options = getOptions(opts);
  var ast = HermesParser.parse(code, options);
  var adapter = getAdapter(options, code);
  return adapter.transform(ast);
}