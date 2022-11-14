# Created by dengshiwei on 2022/04/20.
# Copyright 2015－2022 Sensors Data Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Maven 发布全量包脚本
echo "start ====== unit test coverage, current path = `pwd`"
unitTestReportPath="repo_unitTest"

calculatePercent() {
  reportPath="$PWD/$unitTestReportPath/${1}-reports/html/index.html"
  # 正则提取 missed 数据
  td_rex="</td>"
  regex="<td class=\"ctr1\">\S+</td>"
  ctr1Num=`grep -Eo "$regex" $reportPath`
  result=${ctr1Num//"<td class=\"ctr1\">"/""}
  result=${result//","/""}
  result=${result//$td_rex/","}
  missedMethod=${result#*,}
  missedMethod=${missedMethod#*,}
  missedMethod=${missedMethod%%,*}

  # 正则提取全部数据
  regex="<td class=\"ctr2\">\S+</td>"
  ctr2Num=`grep -Eo "$regex" $reportPath`
  result=${ctr2Num//"<td class=\"ctr2\">"/""}
  result=${result//","/""}
  result=${result//$td_rex/","}
  allMethod=${result#*,}
  allMethod=${allMethod#*,}
  allMethod=${allMethod#*,}
  allMethod=${allMethod#*,}
  allMethod=${allMethod%%,*}

  percent=$(printf "%d%%" $((($allMethod - $missedMethod)*100/$allMethod)))
  if [ "$percent" = "" ]
  then
    echo "0%"
  else
    echo $percent
  fi
}

startUnitTest() {
  # 编译构建
  ./gradlew :${1}:clean
  ./gradlew :${1}:build
  ./gradlew jacocoInit
  # 拷贝文件
  mv "$PWD/${1}/jacoco.exec" "$PWD/${1}/build/outputs/code-coverage/jacoco.exec"
  # 执行分析
  ./gradlew jacocoTestReport
  # 拷贝报告
  mv "$PWD/${1}/build/reports/jacoco/jacocoTestReport/" "$PWD/$unitTestReportPath/${1}-reports"
}

if [ ! -e ${unitTestReportPath} ];then
  mkdir $unitTestReportPath
else
    rm -rf $unitTestReportPath
    mkdir $unitTestReportPath
fi

# 执行单元测试
startUnitTest "module_advert"
advertPercent=`calculatePercent "module_advert"`
startUnitTest "module_autoTrack"
autoTrackPercent=`calculatePercent "module_autoTrack"`
startUnitTest "module_common"
commonPercent=`calculatePercent "module_common"`
startUnitTest "module_encrypt"
encryptPercent=`calculatePercent "module_encrypt"`
startUnitTest "module_exposure"
exposurePercent=`calculatePercent "module_exposure"`
startUnitTest "module_push"
pushPercent=`calculatePercent "module_push"`
startUnitTest "module_visual"
visualPercent=`calculatePercent "module_visual"`

echo "####################################################################"
echo "#                    Android SDK 单元测试覆盖率                      #"
echo "####################################################################"
echo "#               核心模块          $commonPercent                     "
echo "#               广告模块          $advertPercent                     "
echo "#               全埋点模块        $autoTrackPercent                  "
echo "#               可视化模块        $visualPercent                    "
echo "#               曝光模块          $exposurePercent                   "
echo "#               推送模块          $pushPercent                       "
echo "#               加密模块          $encryptPercent                     "
echo "####################################################################"

echo "end ====== unit test coverage"