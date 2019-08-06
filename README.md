# OpenCover-plugin

With this plugin you can publish your OpenCover coverage reports into Jenkins. It implements [code-coverage-api-plugin](https://github.com/jenkinsci/code-coverage-api-plugin) and can generate coverage chart, fail builds using some threshold and show diff between your pull requests and target branches.

Using pipeline:
`publishCoverage adapters: [opencoverAdapter(mergeToOneReport: true, path: 'testing*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')`

To get better how-to information please refer to [this](https://github.com/jenkinsci/code-coverage-api-plugin#how-to-use-it) article.

