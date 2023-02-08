import java.io.File;

import com.jezhumble.javasysmon.CpuTimes;
import com.jezhumble.javasysmon.JavaSysMon;
import com.jezhumble.javasysmon.MemoryStats;

public class SysMon {

	private JavaSysMon sysmon = null;
	
	public SysMon() {
		try {
			System.setProperty("JAVA_SYS_MON_TEMP_DIR", ".");
			sysmon = new JavaSysMon();
			System.clearProperty("JAVA_SYS_MON_TEMP_DIR");
		} catch (Exception e) {
			// NONE
		}
	}

	public String diff_cpu() {
		if(sysmon == null) return null;
		CPU cpu = cpu();
		if(this.cpu == null
		|| this.cpu.usage != cpu.usage) {
			this.cpu = cpu;
			return "cpu " + cpu.usage + "%";
		}
		return null;
	}

	public String diff_mem() {
		if(sysmon == null) return null;
		Memory mem = memory();
		if(this.mem == null
		|| this.mem.usage != mem.usage) {
			this.mem = mem;
			return "mem " + mem.usage + "%";
		}
		return null;
	}

	public String diff_drv() {
		if(sysmon == null) return null;
		Drive drv = drive();
		if(this.drv == null
		|| this.drv.usage != drv.usage) {
			this.drv = drv;
			return "drv " + drv.usage + "%";
		}
		return null;
	}

	public String curr_cpu() {
		if(sysmon == null) return null;
		if(this.cpu != null) {
			return "cpu " + cpu.usage + "%";
		}
		return null;
	}

	public String curr_mem() {
		if(sysmon == null) return null;
		if(this.mem != null) {
			return "mem " + mem.usage + "%";
		}
		return null;
	}

	public String curr_drv() {
		if(sysmon == null) return null;
		if(this.drv != null) {
			return "drv " + drv.usage + "%";
		}
		return null;
	}

	public String getCpu() {
		if(cpu == null) return null;
		return cpu.toString();
	}
	public String getMem() {
		if(mem == null) return null;
		return mem.toString();
	}
	public String getDrv() {
		if(drv == null) return null;
		return drv.toString();
	}

	public class CPU {
		public long sys; /*sec*/
		public long user; /*sec*/
		public long idle; /*sec*/
		public long total; /*sec*/
		public int usage; /* % */
		public int _sys; /* % */
		public int _user; /* % */
		public int _idle; /* % */
		public String toString() {
			String rc = "Usage: " + usage + "%";
			rc += "\nTotal: " + String.format("%,d", total) + " sec";
			rc += "\nUser: " + String.format("%,d", user) + " sec";
			rc += "\nIdle: " + String.format("%,d", idle) + " sec";
			return rc;
		}
	}

	public class Memory {
		public long total; /*mb*/
		public long free; /*mb*/
		public long used; /*mb*/
		public int usage; /* % */
		public String toString() {
			String rc = "Usage: " + usage + "%";
			rc += "\nTotal: " + String.format("%,d", total) + " MB";
			rc += "\nFree: " + String.format("%,d", free) + " MB";
			rc += "\nUsed: " + String.format("%,d", used) + " MB";
			return rc;
		}
	}

	public class Drive {
		public String drive;
		public long total;
		public long free;
		public long used;
		public int usage; /* % */
		public String toString() {
			String rc = drive.toUpperCase().charAt(0) + " Usage: " + usage + "%";
			rc += "\nTotal: " + String.format("%,d", total) + "MB";
			rc += "\nFree: " + String.format("%,d", free) + "MB";
			rc += "\nUsed: " + String.format("%,d", used) + "MB";
			return rc;
		}
	}

	private CpuTimes prev = null; // monitor.cpuTimes();
	private CPU cpu;
	private Memory mem;
	private Drive drv;

	private CPU cpu() {
		CpuTimes ct = sysmon.cpuTimes();
		CPU cpu = new CPU();
		cpu.sys = (long)(ct.getSystemMillis() / 1000f);
		cpu.user = (long)(ct.getUserMillis() / 1000f);
		cpu.idle = (long)(ct.getIdleMillis() / 1000f);
		cpu.total = (long)(ct.getTotalMillis() / 1000f);
		cpu.usage = (prev==null)? 0 : (int)((ct.getCpuUsage(prev)) * 100f + 0.5f); /* % */
		if(prev != null) {
			float t = ((float) (ct.getTotalMillis() - prev.getTotalMillis())) / 100f;
			cpu._sys = (int) (((float) (ct.getSystemMillis() - prev.getSystemMillis())) / t + 0.5f);
			cpu._user = (int) (((float) (ct.getUserMillis() - prev.getUserMillis())) / t + 0.5f);
			cpu._idle = (int) (((float) (ct.getIdleMillis() - prev.getIdleMillis())) / t + 0.5f);
		}
		prev = ct;
		return cpu;
	}

	private Memory memory() {
		MemoryStats ms = sysmon.physical();
		long t, f, u;
		Memory memory = new Memory();
		memory.free = (f = ms.getFreeBytes()) / 1024L / 1024L;
		memory.total = (t = ms.getTotalBytes()) / 1024L / 1024L;
		memory.used = (u = t - f) / 1024L / 1024L;
		memory.usage = (int)((double)u * 100D / (double)t); /* % */
		return memory;
	}
	
	private Drive drive() {
		String drv = "c:\\";
		File file = new File(drv);
		long t, f, u;
		t = file.getTotalSpace();
		Drive drive = new Drive();
		drive.drive = drv;
		drive.total= t / 1024L / 1024L;
		if(t > 0) {
			drive.free = (f = file.getFreeSpace()) / 1024L / 1024L;
			drive.used = (u = t - f) / 1024L / 1024L;
			drive.usage = (int)((double)u * 100D / (double)t); /* % */
		}
		return drive;
	}
}
