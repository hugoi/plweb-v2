import groovy.sql.Sql
import java.text.DecimalFormat
import org.plweb.webapp.helper.CommonHelper

def helper = new CommonHelper(request, response, session)

def sql = new Sql(helper.connection)

// 清除登入錯誤顯示訊息

helper.sess 'login_error', null

// session data
def uid		= helper.sess('uid')
def utype	= helper.sess('utype')

if (!uid) {
	response.sendError 403
	return
}

// 取得表單資料
def email = helper.fetch('email')

// sql queries
query1 = """
select COUNT(*) as USER_COUNT
from ST_USER
where LAST_UPDATE >= ?
and IS_LOGIN='y'
"""

_SQL_CLASS_LIST = """
	select b.*
	from USER_CLASS a
	inner join ST_CLASS b
	on a.CLASS_ID=b.CLASS_ID
	where a.USER_ID=?
	and b.ALIVE='Y'
	and b.SEMESTER<5
	and b.YEARS >= 2010
	order by b.CLASS_ID desc
"""

// rows for class list
classes = []
sql.eachRow(_SQL_CLASS_LIST, [uid]) {
	row ->
	classes << [
		id: row.CLASS_ID,
		name: row.CLASS_NAME,
		school: row.SCHOOL,
		dept: row.DEPARTMENT,
		year: row.YEARS,
		href: response.encodeUrl("dashboard/index.groovy?m=show_class&c=${row.CLASS_ID}")
	]
}

def ucount = 0
try {
	ucount = sql.firstRow(query1, [new Date().time-(300*1000)]).USER_COUNT
}
catch (e) {
}

helper.attr 'admin_url', helper.make_url('admin.system/entry.groovy', [], true)
helper.attr 'is_admin', helper.sess('is_admin')
helper.attr 'ucount', ucount
helper.attr 'classes', classes

helper.forward 'index.gsp'
