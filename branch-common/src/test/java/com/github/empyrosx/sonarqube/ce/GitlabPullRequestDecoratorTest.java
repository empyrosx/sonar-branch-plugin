package com.github.empyrosx.sonarqube.ce;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class GitlabPullRequestDecoratorTest {

    List<String> diffs;

    @Before
    public void setUp() {
        diffs = new ArrayList<>();
    }

    private Integer getOldLine(int line) {
        return DiffUtils.getBaseSourceLine(diffs, line);
    }

    @Test
    public void testLineMap() {
        String diff = "@@ -2,11 +2,13 @@ \n" +
                " \n" +
                " \n" +
                " class Auth:\n" +
                "+    \"\"\" Session \"\"\"\n" +
                " \n" +
                "     _instance = None\n" +
                " \n" +
                "     @classmethod\n" +
                "     def instance(cls):\n" +
                "+        \"\"\" Instance \"\"\"\n" +
                "         if not cls._instance:\n" +
                "             cls._instance = cls()\n" +
                " \n";
        String diff2 = "@@ -25,6 +25,14 @@ \n" +
                "     def set(data):\n" +
                "         Session.instance().data = data\n" +
                " \n" +
                "+\n" +
                "+class ClassWithoutDoc:\n" +
                "+\n" +
                "+    @classmethod\n" +
                "+    def method_without_doc(cls):\n" +
                "+        pass\n" +
                "+\n" +
                "+\n" +
                " def authenticate():\n" +
                "     return 0";
        diffs.add(diff);
        diffs.add(diff2);

        Assert.assertEquals(Integer.valueOf(2), getOldLine(2));
        Assert.assertEquals(Integer.valueOf(4), getOldLine(4));
        Assert.assertNull(getOldLine(5));
        Assert.assertEquals(Integer.valueOf(5), getOldLine(6));
        Assert.assertEquals(Integer.valueOf(23), getOldLine(25));
        Assert.assertEquals(Integer.valueOf(23), getOldLine(25));
        Assert.assertNull(getOldLine(32));
        Assert.assertEquals(Integer.valueOf(26), getOldLine(36));
        Assert.assertEquals(Integer.valueOf(48), getOldLine(58));
    }

    @Test
    public void testMultiBlockDiff() {
        String diff = "@@ -242,6 +242,14 @@ \n" +
                "             }\n" +
                "          });\n" +
                " \n" +
                "+         AA.c('A').s('B', function() {\n" +
                "+            self.C();\n" +
                "+         });\n" +
                "+\n" +
                "+         AA.c('D').s('E', function() {\n" +
                "+            self.F();\n" +
                "+         });\n" +
                "+\n" +
                " \n" +
                "          // Comment \n" +
                "          AA.c('A').s('G', function() {\n" +
                "@@ -276,6 +284,17 @@ \n" +
                "                AA.c('A').n('H');\n" +
                "             }\n" +
                "          },\n" +
                "+         {\n" +
                "+            name: 'L',\n" +
                "+            predicate: function(key, ctrl, shift) {\n" +
                "+               var a_key = 65;\n" +
                "+\n" +
                "+               return (key === a_key && ctrl && shift);\n" +
                "+            },\n" +
                "+            K: function() {\n" +
                "+               AA.c('N').n('M');\n" +
                "+            }\n" +
                "+         },\n" +
                "          {\n" +
                "             name: 'U',\n" +
                "             predicate: function(key, ctrl, shift) {\n" +
                "@@ -483,6 +502,12 @@ \n" +
                "          return this.L[E];\n" +
                "       },\n" +
                " \n" +
                "+      N: function() {\n" +
                "+         $('.M').css('display') === 'none'\n" +
                "+            ? $('.W').fadeIn(100)\n" +
                "+            : $('.E').fadeOut(100);\n" +
                "+      },\n" +
                "+\n" +
                "       II: function() {\n" +
                "          return OO;\n" +
                "       },\n";
        diffs.add(diff);

        Assert.assertEquals(Integer.valueOf(244), getOldLine(244));
        Assert.assertNull(getOldLine(245));
        Assert.assertNull(getOldLine(252));
        Assert.assertEquals(Integer.valueOf(278), getOldLine(286));
        Assert.assertNull(getOldLine(287));
        Assert.assertNull(getOldLine(297));
        Assert.assertEquals(Integer.valueOf(279), getOldLine(298));
        Assert.assertEquals(Integer.valueOf(485), getOldLine(504));
        Assert.assertNull(getOldLine(505));
        Assert.assertNull(getOldLine(510));
        Assert.assertEquals(Integer.valueOf(486), getOldLine(511));
    }


    @Test
    public void test7MultiBlockDiff() {
        diffs.add("@@ -19,6 +19,7 @@ \n" +
                "    'A',\n" +
                "    'B',\n" +
                "    'C',\n" +
                "+   'D',\n" +
                "    'E',\n" +
                "    'F',\n" +
                "    'G',");

        diffs.add("@@ -427,6 +427,7 @@ d(H, [\n" +
                "           * comment \n" +
                "           */\n" +
                "       I: function() {\n" +
                "+         J.K.c('L').n('M');\n" +
                "          var s = t;\n" +
                "          if (this.i()) {\n" +
                "             s.c();\n" +
                "@@ -454,6 +455,7 @@ \n" +
                "        */\n" +
                "       N: function() {\n" +
                "          O.s.P.c(this);\n" +
                "+         R.S.c('T').n('U');\n" +
                "          this.V();\n" +
                " \n" +
                "          // comment ");

        Assert.assertEquals(Integer.valueOf(21), getOldLine(21));
        Assert.assertNull(getOldLine(22));
        Assert.assertEquals(Integer.valueOf(426), getOldLine(427));
        Assert.assertNull(getOldLine(430));
        Assert.assertEquals(Integer.valueOf(429), getOldLine(431));
        Assert.assertEquals(Integer.valueOf(455), getOldLine(457));
        Assert.assertNull(getOldLine(458));
        Assert.assertEquals(Integer.valueOf(456), getOldLine(459));
    }

    @Test
    public void testPopup() {
        diffs.add("@@ -7,10 +7,11 @@ \n" +
                "       'A',\n" +
                "       'B',\n" +
                "       'C',\n" +
                "+      'D',\n" +
                "       'E',\n" +
                "       'F',\n" +
                "       'G'\n" +
                "-   ], function(I) {\n" +
                "+   ], function(J) {\n" +
                "    /**\n" +
                "     * K\n" +
                "     * @class L\n" +
                "@@ -136,12 +137,20 @@ \n" +
                "          },\n" +
                " \n" +
                "          M: function(N) {\n" +
                "+            var r = '',\n" +
                "+               self = this;\n" +
                "             if (s === P) {\n" +
                "                this.R.a('Z');\n" +
                "+               r = 'P';\n" +
                "             } else {\n" +
                "                var r = (s === C ? 'Z' : 'A');\n" +
                "                this.A.a('I');\n" +
                "+               r = 'U' + r;\n" +
                "             }\n" +
                "+            $('.Z').fadeOut(10000, function() {\n" +
                "+               self.U();\n" +
                "+               A.B.c('D').n('U', r, self._options.data)\n" +
                "+            })\n" +
                "          },\n" +
                "          U: function() {\n" +
                "             if (this._options.T) {\n");

        Assert.assertEquals(Integer.valueOf(9), getOldLine(9));
        Assert.assertNull(getOldLine(10));
        Assert.assertEquals(Integer.valueOf(10), getOldLine(11));
        Assert.assertEquals(Integer.valueOf(12), getOldLine(13));
        Assert.assertNull(getOldLine(14));
        Assert.assertEquals(Integer.valueOf(14), getOldLine(15));
        Assert.assertEquals(Integer.valueOf(138), getOldLine(139));
        Assert.assertNull(getOldLine(140));
        Assert.assertEquals(Integer.valueOf(139), getOldLine(142));
        Assert.assertNull(getOldLine(144));
        Assert.assertEquals(Integer.valueOf(143), getOldLine(147));
        Assert.assertNull(getOldLine(148));
        Assert.assertEquals(Integer.valueOf(144), getOldLine(149));
        Assert.assertNull(getOldLine(150));
        Assert.assertNull(getOldLine(151));
        Assert.assertNull(getOldLine(152));
        Assert.assertNull(getOldLine(153));
        Assert.assertEquals(Integer.valueOf(145), getOldLine(154));

        diffs.add("@@ -137,19 +137,16 @@ \n" +
                "          },\n" +
                " \n" +
                "          H: function(s) {\n" +
                "-            var r = '',\n" +
                "                self = this;\n" +
                "             if (s === Y) {\n" +
                "                this.J.a('W U');\n" +
                "-               r = 'W U';\n" +
                "             } else {\n" +
                "                var r = (s === G ? 'F__type_warning' : 'F__type_error');\n" +
                "                this.X.a('E F__type-icon ' + r);\n" +
                "-               r = 'E F__type-icon ' + r;\n" +
                "             }\n" +
                "-            $('.F').fadeOut(10000, function() {\n" +
                "+            $('[id=\"' + self._id + '\"]').fadeOut(10000, function() {\n" +
                "                self._c();\n" +
                "-               A.B.c('C').n('D', r, self._options.data)\n" +
                "+               A.B.c('C').n('D', self._options.data)\n" +
                "             })\n" +
                "          },\n" +
                "          O: function() {\n");

        Assert.assertEquals(Integer.valueOf(9), getOldLine(9));
        Assert.assertNull(getOldLine(10));
        Assert.assertEquals(Integer.valueOf(10), getOldLine(11));
        Assert.assertEquals(Integer.valueOf(12), getOldLine(13));
        Assert.assertNull(getOldLine(14));
        Assert.assertEquals(Integer.valueOf(14), getOldLine(15));
        Assert.assertEquals(Integer.valueOf(138), getOldLine(139));
        Assert.assertNull(getOldLine(140));
        Assert.assertEquals(Integer.valueOf(140), getOldLine(142));
        Assert.assertEquals(Integer.valueOf(141), getOldLine(143));
        Assert.assertEquals(Integer.valueOf(142), getOldLine(144));
        Assert.assertEquals(Integer.valueOf(143), getOldLine(145));
        Assert.assertEquals(Integer.valueOf(144), getOldLine(146));
        Assert.assertNull(getOldLine(147));
        Assert.assertNull(getOldLine(148));
        Assert.assertNull(getOldLine(149));
        Assert.assertNull(getOldLine(150));
        Assert.assertEquals(Integer.valueOf(145), getOldLine(151));
    }

    @Test
    public void simpleTest01() {
        diffs.add("@@ -215,6 +215,11 @@ \n" +
                "          A.B.c('C').s('D', function(e, useDefault) {\n" +
                "             Storage.store('X')\n" +
                "          });\n" +
                "+\n" +
                "+         A.B.c('C').s('D', function(e, s) {\n" +
                "+            P.store('footers', s == 'R' ? 'E' : 'D');\n" +
                "+         });\n" +
                "+\n" +
                "          A.B.c('X').s('F', function(e, f) {\n" +
                "             self.P.O().clear();\n" +
                "             if (Y == 'X') {\n");

        diffs.add("@@ -22,6 +22,7 @@ \n" +
                "    'A',\n" +
                "    'V',\n" +
                "    'F',\n" +
                "+   'B',\n" +
                "    'A',\n" +
                "    'S',\n" +
                "    'G',\n" +
                "@@ -36,7 +37,7 @@ \n" +
                "    'I'\n" +
                " ], function(Z,\n" +
                "    P,\n" +
                "-   D) {\n" +
                "+   E) {\n" +
                "    /**\n" +
                "     * \n" +
                "     * @class U\n" +
                "@@ -46,7 +47,7 @@ \n" +
                "       _X: X,\n" +
                "       $P: {\n" +
                "          _options: {},\n" +
                "-\n" +
                "+         _C: null,\n" +
                "          _P: null,\n" +
                "          _D: null,\n" +
                "          _L: null,\n" +
                "@@ -78,7 +79,6 @@ \n" +
                "          this._D = new D({\n" +
                "             p: this\n" +
                "          });\n" +
                "-\n" +
                "          // comment \n" +
                "          this._S = new S({\n" +
                "             e: $('<div></div>').appendTo(this.C()),\n" +
                "@@ -119,6 +119,10 @@ \n" +
                " \n" +
                "          this._o = this.g('s');\n" +
                " \n" +
                "+         this._C = new C({\n" +
                "+            s: this._o\n" +
                "+         });\n" +
                "+\n" +
                "          this._w = new W(w);\n" +
                " \n" +
                "          this.u();");

        diffs.add("@@ -79,7 +79,6 @@ \n" +
                "          this._d = new D({\n" +
                "             p: this\n" +
                "          });\n" +
                "-         // comment \n" +
                "          this._s = new S({\n" +
                "             e: $('<div></div>').a(t.g()),\n" +
                "             p: this");


        Assert.assertNull(getOldLine(223));
    }
}