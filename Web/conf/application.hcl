ServerName = VoovanWebServer
ScanPaths[
  org.voovan.test.tools.ioc
]
//WebServer 配置文件引入
Web = "@web.hcl"
//其他配置引入
// Consul : "#http://127.0.0.1:8500/v1/kv/Test/application.json?raw"