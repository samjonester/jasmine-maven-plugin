var fileSystemReporter;

(function() {
  var resultForSpec = function(reporter, spec){
    var specResults = reporter.specs();
    for (var i=0; i < specResults.length; i++) {
      if (spec.id == specResults[i].id) {
        return specResults[i];
      }
    }
    return {};
  };

  fileSystemReporter = {
    prolog: '<?xml version="1.0" encoding="UTF-8" ?>',
    report: function(reporter,debug) {
      if (!reporter)
        throw 'Jasmine JS API Reporter must not be null.';
      if (reporter.finished !== true && !debug)
        throw 'Jasmine runner is not finished!';

      var results = this.crunchResults(reporter.specs());

      var writer = new XmlWriter();
      writer.beginNode('testsuite');
      writer.attrib('errors','0');
      writer.attrib('name','jasmine.specs');
      writer.attrib('tests',results.tests);
      writer.attrib('failures',results.failures);
      writer.attrib('skipped',results.skipped);
      writer.attrib('hostname','localhost');
      writer.attrib('time', '0.0');
      writer.attrib('timestamp',this.currentTimestamp());
      this.writeChildren(reporter, writer, jasmine.getEnv().topSuite().children,'');
      writer.endNode();

      return this.prolog+writer.toString();
    },
    writeChildren: function(reporter, writer, tests,runningName) {
      if (tests) {
        for(var i=0;i<tests.length;i++) {
          var name = (runningName && runningName.length > 0 ? runningName+' ' : '')+tests[i].description;
          if(tests[i] instanceof jasmine.Spec) {
            var specResult = resultForSpec(reporter, tests[i]);
            this.writeTestcase(writer,specResult,name);
          }
          this.writeChildren(reporter, writer,tests[i].children,name);
        }
      }
    },
    writeTestcase: function(writer,specResult,name) {
      var failure = specResult.status == 'failed';
      var skipped = specResult.status == 'pending';
      writer.beginNode('testcase');
      writer.attrib('classname','jasmine');
      writer.attrib('name',name);
      writer.attrib('time',''+(specResult.time || '0.0'));

      if(skipped) {
        this.writeSkipped(writer);
      } else {
        writer.attrib('failure',failure+'');
        if(failure) {
          this.writeError(writer,specResult);
        }
      }
      writer.endNode();
    },
    writeError: function(writer,specResult) {
      writer.beginNode('error');
      var message = '';
      var type = '';
      var messages = specResult.failedExpectations || [];
      for(var j=0;j<messages.length;j++) {
        message += messages[j].message;
        type = 'expect.' + messages[j].matcherName;
      }
      writer.attrib('type',type);
      writer.attrib('message',message);
      writer.writeString(message);
      writer.endNode();
    },
    writeSkipped: function(writer) {
      writer.beginNode('skipped');
      writer.endNode();
    },
    crunchResults: function(results) {
      var count=0;
      var fails=0;
      var skipped=0;
      var last;
      for(var key in results) {
        count++;
        if(results[key].status == 'failed') {
          fails++;
        }
        if(results[key].status == 'pending') {
          skipped++;
        }
        last = key;
      }
      return {
        tests: count.toString(),
        failures: fails.toString(),
        skipped: skipped.toString()
      };
    },
    currentTimestamp: function() {
      var f = function(n) {
            // Format integers to have at least two digits.
            return n < 10 ? '0' + n : n;
        }

      var date = new Date();

          return date.getUTCFullYear()   + '-' +
               f(date.getUTCMonth() + 1) + '-' +
               f(date.getUTCDate())      + 'T' +
               f(date.getUTCHours())     + ':' +
               f(date.getUTCMinutes())   + ':' +
               f(date.getUTCSeconds());
    }
  };

  //From here: http://www.codeproject.com/KB/ajax/XMLWriter.aspx
  function XmlWriter() {
    this.XML = [];
    this.nodes = [];
    this.State = "";
    this.formatXml = function(Str) {
      if (Str)
        return Str.replace(/&/g, "&amp;").replace(/\"/g, "&quot;")
            .replace(/</g, "&lt;").replace(/>/g, "&gt;");
      return ""
    }
    this.beginNode = function(Name) {
      if (!Name)
        return;
      if (this.State == "beg")
        this.XML.push(">");
      this.State = "beg";
      this.nodes.push(Name);
      this.XML.push("<" + Name);
    }
    this.endNode = function() {
      if (this.State == "beg") {
        this.XML.push("/>");
        this.nodes.pop();
      } else if (this.nodes.length > 0)
        this.XML.push("</" + this.nodes.pop() + ">");
      this.State = "";
    }
    this.attrib = function(Name, Value) {
      if (this.State != "beg" || !Name)
        return;
      this.XML.push(" " + Name + "=\"" + this.formatXml(Value) + "\"");
    }
    this.writeString = function(Value) {
      if (this.State == "beg")
        this.XML.push(">");
      this.XML.push(this.formatXml(Value));
      this.State = "";
    }
    this.node = function(Name, Value) {
      if (!Name)
        return;
      if (this.State == "beg")
        this.XML.push(">");
      this.XML.push((Value == "" || !Value) ? "<" + Name + "/>" : "<"
          + Name + ">" + this.formatXml(Value) + "</" + Name + ">");
      this.State = "";
    }
    this.close = function() {
      while (this.nodes.length > 0)
        this.endNode();
      this.State = "closed";
    }
    this.toString = function() {
      return this.XML.join("");
    }
  }

})();
