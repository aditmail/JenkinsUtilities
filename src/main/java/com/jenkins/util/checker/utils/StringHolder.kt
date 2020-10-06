package com.jenkins.util.checker.utils

const val strStrips = "-----------------------------------"
const val strAsterisks = "***************"

const val strConfigValidator = "$strAsterisks CONFIG VALIDATOR $strAsterisks"
const val strDeployValidator = "$strAsterisks DEPLOY VALIDATOR $strAsterisks"
const val strProjectName = "Project Name"
const val strFlavorType = "Flavor-Type"
const val strNodeQuantity = "Node Quantity"
const val strDeployModel = "Deployment Models"
const val strGeneratedAt = "&#8258; Generated At: <b><mark>%1s</mark></b>"

const val strNodeNo = "Node No."
const val strNodeName = "Node Name"

const val strDirFoundsHTML = "%1s %2s Found!<br>$strStrips"

const val strConfigNameHTML = "&#8251; %1s <u>Config</u> &#8658; <b>%2s</b>"
const val strInstanceNameHTML = "<b>&#8656; &#8251;%1s Instance &#8658;</b>"
const val strDeploymentNameHTML = "<b>&#8656; <u style=\"color:blue\">%1s</u> Deployment &#8658;</b><br>$strStrips"
const val strPathName = "Path Name"

const val strNo = "No"
const val strName = "Name"
const val strSize = "Size"
const val strMD5Code = "MD5 Code"
const val strNotes = "&#8258;<u>NOTES</u>"

const val strErrorPathName = "Error Path Name"
const val strExpectedHTML = "Expected &#9989;"
const val strFoundHTML = "Found &#10060;"

const val strStatus = "Status"
const val strPassed = "PASSED"
const val strFailed = "FAILED"
const val strErrorList = "ERROR LIST"

const val strConfigPassedHTML = "&#8258;<u>NOTES</u>:: <b>%1s %2s</b> from Config (.txt) %3s <b>Successfully Mapped</b> to Selected Directories &#9989;"
const val strDeploymentPassedHTML = "<b>%1s</b> is <b>Successfully Mapped</b> to Selected Directories &#9989;"
const val strDeploymentFailedHTML = "<b>%1s</b> is <b>Is not the Correct Files</b> which Mapped to Selected Directories &#10060;"

const val strConfigErrorNotMappingHTML = "*<b style=\"color:red\">ERROR</b> &#8658; There's <b>%1s Data from Config</b> (.txt) That %2s <b>NOT Mapping</b> to Selected Directories &#10060;"
const val strConfigErrorNotBasedHTML = "*<b style=\"color:red\">ERROR</b> &#8658; There's <b>%1s Data</b> That %2s <b>NOT Based on the Config</b> (.txt) Mapped to Selected Directories &#10060;"
const val strConfigErrorNotesHTML = "*<b>NOTES</b> &#8658; <mark> <b>Expected &#8614; %1s Mapped</b></mark> &#9474; "
const val strConfigErrorNotesActualHTML = "<b>Actual &#8614; %1s Mapped </b>"
const val strConfigErrorExpectingHTML = "&#8258;<b>EXPECTING</b> &#8658; <mark><b>%1s</b></mark> but Found <b>%2s</b> &#10071;"
const val strConfigErrorActionHTML = "&#8258;<b>ACTION</b> &#8658; Please <b>Check the Path/Jenkins Configuration</b> Again for Correction/Validation"

const val strListItemHTML = "&#8656; List Item:: <b>%1s Found</b> &#8658;"
const val strReportSummaries = "$strAsterisks REPORT SUMMARIES $strAsterisks"

const val strTotalData = "Total Data"
const val strTotalPassedData = "Total Passed Data"
const val strTotalFailedData = "Total Failed Data"

const val strStyleHTML = """
    <style>
        table, th, td {
            border-collapse: initial;
            text-align: left;
            vertical-align: middle;
            padding: 8px;
            border-radius: 4px;
        }
        
        div{
            padding: 16px;
            display:inline-block;
            border-radius: 8px;
        }
        
        div.pj{
            background-color: #ffe6cc;
        }
        
        div.content{
            background-color: #f2f2f2;
        }
        
        th.center{
            vertical-align: middle;
            text-align: center;
        }
        
        td.center{
            text-align: center;
        }

        h4{
            margin-top: 16px;
            margin-bottom: 16px;
        }
        
        h3 {
            margin-top: 4px;
            margin-bottom: 4px;
        }
        
        p{
            margin-top: 4px;    
            margin-bottom: 4px;
        }
        
        p.percentage{
            font-weight: bold;
            font-size: 24px;
        }
        
        p.passed{
            font-weight: bold;
            color: green;
        }
        
        p.failed{
            font-weight: bold;
            color: red;
        }
        
        p.listData{
            font-weight: bold;
            color: blue;
        }
        
        p.tableData{
            font-weight: bold;
        }
    </style>"""



