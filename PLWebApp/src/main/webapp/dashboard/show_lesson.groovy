/**
 * @require js/jquery.poshytip.min.js
 */

import groovy.sql.Sql
import org.plweb.webapp.helper.CommonHelper
import java.text.DecimalFormat

helper = new CommonHelper(request, response, session)

uid		= helper.sess('uid')
utype	= helper.sess('utype')

if (!uid) {
	println """權限不足，請重新<a href=\"${helper.basehref}login/\">登入</a>。"""
	return
}


class_id		= helper.fetch('c')
panel_lesson_id	= helper.fetch('l')

course_id	= null
lesson_id	= null

if (panel_lesson_id) {
	tokens = panel_lesson_id.split(',')
	
	course_id	= tokens[0]
	lesson_id	= tokens[1]
}

sql = new Sql(helper.connection)

// detect permissions
row = sql.firstRow("""
	select IS_TEACHER
	from USER_CLASS
	where USER_ID=?
	and CLASS_ID=?
""", [uid, class_id])

if (!row) {
	println "您尚未參與本課程。"
	return
}

is_teacher = 'y'.equalsIgnoreCase(row.IS_TEACHER)

// 日期格式, 讀取DUEDATE設定
row = sql.firstRow("""
	select BEGINDATE, DUEDATE, CLOSEDATE, IS_SHOW_ANSWER
	from CLASS_COURSE
	where class_id=?
	and course_id=?
	and lesson_id=?
""", [class_id, course_id, lesson_id])

begin_date = '????/??/??'
begin_time = '??:??:??'
due_date = '????/??/??'
due_time = '??:??:??'
close_date = '????/??/??'
close_time = '??:??:??'

if (row.BEGINDATE != null) {
	begindate = new Date(row.BEGINDATE.toLong())
	begin_date = begindate.format(helper.dateFormat)
	begin_time = begindate.format(helper.timeFormat)
}

if (row.DUEDATE != null) {
	duedate = new Date(row.DUEDATE.toLong())
	due_date = duedate.format(helper.dateFormat)
	due_time = duedate.format(helper.timeFormat)
}

if (row.CLOSEDATE != null) {
	closedate = new Date(row.CLOSEDATE.toLong())
	close_date = closedate.format(helper.dateFormat)
	close_time = closedate.format(helper.timeFormat)
}

is_show_answer = 'y'.equalsIgnoreCase(row.IS_SHOW_ANSWER)

allow_editor = false
editor_url = helper.make_url('webstart.groovy', [mode: 'student', course_id: course_id, lesson_id: lesson_id, class_id: class_id, cdn: true, '_t': new Date().time], true)

//EXCEL WS HACK
if (course_id=='206') { 
    editor_url = helper.make_url('http://140.125.251.188:8080/webstart.php', [mode: 'student', course_id: course_id, lesson_id: lesson_id, class_id: class_id, cdn: true, '_t': new Date().time], false)
}

now = new Date().time

if (is_teacher) {
	allow_editor = true
}
else if (row.BEGINDATE != null && row.BEGINDATE.toLong() <= now) {
	if (row.CLOSEDATE == null || row.CLOSEDATE.toLong() > now) {
		allow_editor = true
	}
}

allow_answer = false
answer_url = null

if (is_teacher || (row.DUEDATE != null && row.DUEDATE.toLong() <= now)) {
	allow_answer = true
	answer_url = "${helper.basehref}dashboard/answer.groovy?course_id=${course_id}&lesson_id=${lesson_id}"
	//answer_url = helper.make_url('webstart.groovy', [mode: 'answer', course_id: course_id, lesson_id: lesson_id, class_id: class_id], true)
}

//辨認測驗模式設定
exam_type = false

row = sql.firstRow("""
	select SEMESTER
	from ST_CLASS
	where class_id=?
""", [class_id])

if (row) {
	exam_type = row.SEMESTER.equals(5)
}

// query CLASS_COURSE.HTML_TEXT
row = sql.firstRow("""
	select TITLE, HTML_TEXT
	from CLASS_COURSE
	where CLASS_ID=?
	and COURSE_ID=?
	and LESSON_ID=?
""", [class_id, course_id, lesson_id]);

if (row.HTML_TEXT) {
	html_text = row.HTML_TEXT
}
else {
	html_text = "<p>教材內容未設定！</p>"
}

helper.attr 'title', row.TITLE

//html_content = "no contents"
//html_content = helper.http_fetch('http://wiki.plweb.org/index.php?title=Scheme/Activate_Thinking_in_Scheme/Chapter01&action=render')

helper.attr 'html_text',	html_text
helper.attr 'report_url',	"${helper.basehref}panel.lesson/classroom_report.groovy?class_id=${class_id}&course_id=${course_id}&lesson_id=${lesson_id}"
helper.attr 'date_url',		"${helper.basehref}panel.lesson/classroom_date.groovy?class_id=${class_id}"
helper.attr 'edithtml_url', "${helper.basehref}dashboard/edithtml.groovy?c=${class_id}&l=${course_id},${lesson_id}"

helper.attr 'disqus_identifier', "comments-${class_id}-${course_id}-${lesson_id}"

helper.attr 'allow_editor',	allow_editor
helper.attr 'editor_url',	editor_url

helper.attr 'allow_answer',	allow_answer
helper.attr 'answer_url',	answer_url

helper.attr 'begin_date',	begin_date
helper.attr 'begin_time',	begin_time
helper.attr 'due_date',		due_date
helper.attr 'due_time',		due_time

helper.attr 'close_date',	close_date
helper.attr 'close_time',	close_time

helper.attr 'exam_type',	exam_type
helper.attr 'class_id',		class_id
helper.attr 'course_id',	course_id
helper.attr 'lesson_id',	lesson_id

helper.attr 'is_teacher',		is_teacher
helper.attr 'is_show_answer',	is_show_answer

helper.include 'show_lesson.gsp'
