"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = isInstalled;

function _cliTools() {
  const data = require("@react-native-community/cli-tools");

  _cliTools = function () {
    return data;
  };

  return data;
}

function _fs() {
  const data = _interopRequireDefault(require("fs"));

  _fs = function () {
    return data;
  };

  return data;
}

var _makeBuildPatch = _interopRequireDefault(require("./patches/makeBuildPatch"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 */
function isInstalled(config, name) {
  let buildGradle;

  if (!_fs().default.existsSync(config.buildGradlePath)) {
    // Handle default build.gradle path for Gradle Kotlin DSL
    if (!_fs().default.existsSync(config.buildGradlePath + '.kts')) {
      throw new (_cliTools().CLIError)('Cannot resolve build.gradle file at: ' + config.buildGradlePath);
    } else {
      buildGradle = _fs().default.readFileSync(config.buildGradlePath + '.kts', 'utf8');
    }
  } else {
    buildGradle = _fs().default.readFileSync(config.buildGradlePath, 'utf8');
  }

  return (0, _makeBuildPatch.default)(name).installPattern.test(buildGradle);
}

//# sourceMappingURL=isInstalled.js.map