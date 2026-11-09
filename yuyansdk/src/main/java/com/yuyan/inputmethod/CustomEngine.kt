package com.yuyan.inputmethod

import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.utils.StringUtils
import com.yuyan.imemodule.utils.TimeUtils
import com.yuyan.imemodule.libs.expression.ExpressionBuilder
import com.yuyan.imemodule.prefs.AppPrefs.Companion.getInstance
import kotlin.math.roundToInt

object CustomEngine {
    fun parseExpressionAtEnd(input: String): String? {
        val expressionEndPattern = "((φ|π|pi|sin|cos|tan|cot|asin|acos|atan|sinh|cosh|tanh|abs|log|log1p|ceil|floor|sqrt|cbrt|pow|exp|expm|signum|csc|sec|csch|sech|coth|toradian|todegree|\\(|\\)|^|%|\\+|-|\\*|/|\\.|e|E|\\d)+)$".toRegex()
        return expressionEndPattern.find(input.removeSuffix("="))?.value
    }

    fun expressionCalculator(input: String, expression: String?):Array<String>{
        val results = mutableListOf<String>()
        if(!expression.isNullOrBlank() && expression.length < 500 && !StringUtils.isNumber(expression) && !StringUtils.isLetter(expression)){
            try {
                val evaluate = ExpressionBuilder(expression).build().evaluate()
                val  resultFloat = evaluate.toFloat()
                val  resultInt = evaluate.roundToInt()
                if(evaluate.compareTo(resultInt) == 0){
                    if(!input.endsWith("=")) results.add("=${resultInt}")
                    else results.add(resultInt.toString())
                } else {
                    val resultPercentage = "${(evaluate * 100).roundToInt()}%"
                    if(!input.endsWith("=")) {
                        results.add("=".plus(resultFloat))
                        results.add("≈".plus(resultInt))
                        if(evaluate < 10 && evaluate > 0.005) results.add("=${resultPercentage}")
                    } else {
                        results.add(resultFloat.toString())
                        if(evaluate < 10 && evaluate > 0.005) results.add(resultPercentage)
                    }
                }
            } catch (_:Exception){ }
        }
        results.addAll(arrayOf("=", "+", "-", "*", "/", "%", ".", ",", "'", "(", ")"))
        return results.toTypedArray()
    }

    fun predictAssociationWordsChinese(text: String): MutableList<String> {
        val trimmed = text.trim()
        val lastChar = if (trimmed.isNotEmpty()) trimmed.last() else ' '
        val isPunctuation = lastChar in "。！？；：，、,.!?;:\"'()[]《》（）【】 "
        val associations = mutableListOf<String>()
        val suffixesDays = setOf("大前天", "前天", "昨天", "今天", "明天", "大后天", "后天")
        val suffixesExclamation = setOf("啊", "呀", "呐", "啦", "噢", "哇", "吧", "呗", "了")
        val suffixesQuestion = setOf("吗", "啊", "呢", "吧", "谁", "何", "什么", "哪", "几", "多少", "怎", "难道", "岂", "不")
        val days = suffixesDays.firstOrNull { text.endsWith(it) }
        if (days != null) {
            associations.addAll(0, TimeUtils.getData(days))
        } else if (!isPunctuation && suffixesExclamation.any { text.endsWith(it) }) {
            associations.add(0, "！")
        } else if (!isPunctuation && suffixesQuestion.any { text.contains(it) }) {
            associations.add(0, "？")
        }
        return associations
    }

    fun processPhrase(text: String): MutableList<String> {
        val phrases = mutableListOf<String>()
        val cleanText = text.replace("'", "").trim()
        if (cleanText.isNotEmpty()) {
            // 查询 9键(34944)、LX17(GXXD/gxxd) 或 26键(dizhi) 精确匹配短语
            val matchedPhrases = DataBaseKT.instance.phraseDao().query(cleanText)
                .ifEmpty { DataBaseKT.instance.phraseDao().query(cleanText.uppercase()) }
                .ifEmpty { DataBaseKT.instance.phraseDao().query(cleanText.lowercase()) }
                .map { it.content }
            phrases.addAll(matchedPhrases)

            val chinesePredictionDate = getInstance().input.chinesePredictionDate.getValue()
            if (chinesePredictionDate) {
                val suffixesDate = setOf("rq", "riqi", "pgpg", "pp", "nq", "njqj")
                if (suffixesDate.any { it.equals(cleanText, ignoreCase = true) }) {
                    phrases.addAll(TimeUtils.getData())
                }
                val suffixesTime = setOf("sj", "shijian", "pj", "pggjgam", "sjjq", "mj", "mhjjjhn", "mjmj", "djmj", "uijm", "iijm", "vijw")
                if (suffixesTime.any { it.equals(cleanText, ignoreCase = true) }) {
                    phrases.addAll(TimeUtils.getTime())
                }
            }
        }
        return phrases
    }
}
